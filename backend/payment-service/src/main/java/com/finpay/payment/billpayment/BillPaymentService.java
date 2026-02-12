package com.finpay.payment.billpayment;

import com.finpay.payment.billpayment.dto.BillPaymentRequest;
import com.finpay.payment.billpayment.dto.BillPaymentResponse;
import com.finpay.payment.billpayment.event.BillPaymentEvent;
import com.finpay.payment.shared.exception.PaymentException;
import com.finpay.payment.shared.exception.ResourceNotFoundException;
import com.finpay.payment.shared.kafka.WalletCommandProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrates the bill-payment lifecycle using the Saga pattern.
 *
 * Flow:
 * 1. User initiates bill payment → record saved as PENDING
 * 2. RESERVE_FUNDS command sent to wallet-service via Kafka
 * 3. Wallet-service responds with FUNDS_RESERVED
 * 4. DEDUCT_FUNDS command sent, wallet-service responds with FUNDS_DEDUCTED
 * 5. Biller confirmation simulated → COMPLETED
 * 6. On any failure → compensation (release / reverse) triggered automatically
 *
 * All state transitions publish events to Kafka for the notification-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillPaymentService {

    private final BillPaymentRepository billPaymentRepository;
    private final BillPaymentEventProducer eventProducer;
    private final WalletCommandProducer walletCommandProducer;

    private static final BigDecimal BILL_FEE_RATE = new BigDecimal("0.005"); // 0.5 %
    private static final BigDecimal MIN_BILL_FEE = new BigDecimal("0.25");

    // Initiate

    public BillPaymentResponse initiateBillPayment(BillPaymentRequest request) {
        log.info("Initiating bill payment for user: {} biller: {} amount: {}",
                request.userId(), request.billerCode(), request.amount());

        String txRef = generateTransactionReference();
        BigDecimal fee = calculateFee(request.amount());
        BigDecimal total = request.amount().add(fee);

        BillPayment bill = BillPayment.builder()
                .userId(request.userId())
                .transactionReference(txRef)
                .category(request.category())
                .billerName(request.billerName())
                .billerCode(request.billerCode())
                .accountNumber(request.accountNumber())
                .accountHolderName(request.accountHolderName())
                .amount(request.amount())
                .currency(request.effectiveCurrency())
                .processingFee(fee)
                .totalAmount(total)
                .status(BillPayment.BillPaymentStatus.PENDING)
                .sagaStatus(BillPayment.SagaStatus.INITIATED)
                .description(request.description())
                .build();

        BillPayment saved = billPaymentRepository.save(bill);
        log.info("Bill payment created: {} ref: {}", saved.getId(), txRef);

        // Publish initiated event
        eventProducer.sendBillPaymentEvent(
                BillPaymentEvent.of(saved, BillPaymentEvent.EventType.BILL_PAYMENT_INITIATED));

        // Saga step 1 → reserve funds from user wallet
        walletCommandProducer.reserveFunds(
                saved.getId(),
                saved.getUserId(),
                saved.getTotalAmount(),
                saved.getCurrency(),
                "Bill payment reserve: " + saved.getBillerName() + " (" + txRef + ")"
        );

        return BillPaymentResponse.fromEntity(saved);
    }

    // Saga callbacks (called by WalletResponseConsumer)

    /**
     * Saga step 1 completed: funds reserved → deduct funds.
     */
    public void handleFundsReserved(UUID billId, UUID walletId) {
        BillPayment bill = findBillOrThrow(billId);
        bill.setFundsReserved(true);
        bill.setWalletId(walletId);
        bill.setSagaStatus(BillPayment.SagaStatus.FUNDS_RESERVED);
        bill.setStatus(BillPayment.BillPaymentStatus.PROCESSING);
        billPaymentRepository.save(bill);

        eventProducer.sendBillPaymentEvent(
                BillPaymentEvent.of(bill, BillPaymentEvent.EventType.BILL_PAYMENT_PROCESSING));

        // Saga step 2 → deduct funds
        walletCommandProducer.deductFunds(
                bill.getId(),
                bill.getUserId(),
                bill.getTotalAmount(),
                bill.getCurrency(),
                "Bill payment deduction: " + bill.getBillerName()
        );
    }

    /**
     * Saga step 2 completed: funds deducted → confirm with biller.
     */
    public void handleFundsDeducted(UUID billId) {
        BillPayment bill = findBillOrThrow(billId);
        bill.setFundsDeducted(true);
        bill.setSagaStatus(BillPayment.SagaStatus.FUNDS_DEDUCTED);
        billPaymentRepository.save(bill);

        // Saga step 3 → simulate biller confirmation
        confirmWithBiller(bill);
    }

    /**
     * Handle wallet operation failure → trigger compensation.
     */
    public void handleFailure(UUID billId, String reason) {
        BillPayment bill = findBillOrThrow(billId);
        bill.setStatus(BillPayment.BillPaymentStatus.FAILED);
        bill.setSagaStatus(BillPayment.SagaStatus.FAILED);
        bill.setFailureReason(reason);
        bill.setFailedAt(LocalDateTime.now());
        bill.setCompensationRequired(true);
        billPaymentRepository.save(bill);

        eventProducer.sendBillPaymentEvent(
                BillPaymentEvent.of(bill, BillPaymentEvent.EventType.BILL_PAYMENT_FAILED));

        startCompensation(bill);
    }

    /**
     * Compensation: funds released after rollback.
     */
    public void handleCompensated(UUID billId) {
        BillPayment bill = findBillOrThrow(billId);
        bill.setCompensationCompleted(true);
        bill.setSagaStatus(BillPayment.SagaStatus.COMPENSATED);
        bill.setStatus(BillPayment.BillPaymentStatus.COMPENSATED);
        billPaymentRepository.save(bill);
    }

    // Queries

    @Transactional(readOnly = true)
    public BillPaymentResponse getBillPayment(UUID id) {
        return BillPaymentResponse.fromEntity(findBillOrThrow(id));
    }

    @Transactional(readOnly = true)
    public BillPaymentResponse getBillPaymentByReference(String ref) {
        BillPayment bill = billPaymentRepository.findByTransactionReference(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Bill payment not found: " + ref));
        return BillPaymentResponse.fromEntity(bill);
    }

    @Transactional(readOnly = true)
    public Page<BillPaymentResponse> getBillPaymentsByUser(UUID userId, Pageable pageable) {
        return billPaymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(BillPaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<BillPaymentResponse> getBillPaymentsByCategory(
            UUID userId, BillPayment.BillCategory category, Pageable pageable) {
        return billPaymentRepository.findByUserIdAndCategory(userId, category, pageable)
                .map(BillPaymentResponse::fromEntity);
    }

    // Cancel

    public BillPaymentResponse cancelBillPayment(UUID id) {
        BillPayment bill = findBillOrThrow(id);
        if (bill.getStatus() != BillPayment.BillPaymentStatus.PENDING) {
            throw new PaymentException("Only pending bill payments can be cancelled");
        }
        bill.setStatus(BillPayment.BillPaymentStatus.CANCELLED);
        billPaymentRepository.save(bill);

        eventProducer.sendBillPaymentEvent(
                BillPaymentEvent.of(bill, BillPaymentEvent.EventType.BILL_PAYMENT_CANCELLED));

        return BillPaymentResponse.fromEntity(bill);
    }

    // Billers catalogue (static for v1)

    /**
     * Returns available bill categories for the UI.
     */
    @Transactional(readOnly = true)
    public BillPayment.BillCategory[] getAvailableCategories() {
        return BillPayment.BillCategory.values();
    }

    // Internal helpers

    private void confirmWithBiller(BillPayment bill) {
        // In production this would call an external biller API.
        // For now, simulate instant confirmation.
        log.info("Confirming payment with biller {} for bill {}", bill.getBillerCode(), bill.getId());

        bill.setBillerConfirmed(true);
        bill.setBillerReference("BILLER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        bill.setBillerResponse("SUCCESS");
        bill.setSagaStatus(BillPayment.SagaStatus.COMPLETED);
        bill.setStatus(BillPayment.BillPaymentStatus.COMPLETED);
        bill.setProcessedAt(LocalDateTime.now());
        billPaymentRepository.save(bill);

        eventProducer.sendBillPaymentEvent(
                BillPaymentEvent.of(bill, BillPaymentEvent.EventType.BILL_PAYMENT_COMPLETED));

        log.info("Bill payment completed: {} ref: {}", bill.getId(), bill.getTransactionReference());
    }

    private void startCompensation(BillPayment bill) {
        log.warn("Starting compensation for failed bill payment {}", bill.getId());

        bill.setStatus(BillPayment.BillPaymentStatus.COMPENSATING);
        bill.setSagaStatus(BillPayment.SagaStatus.COMPENSATING);
        billPaymentRepository.save(bill);

        if (bill.isFundsDeducted()) {
            // Reverse the deduction
            walletCommandProducer.reverseDeduction(
                    bill.getId(), bill.getUserId(), bill.getTotalAmount(),
                    bill.getCurrency(), "Reversal for failed bill: " + bill.getTransactionReference());
        } else if (bill.isFundsReserved()) {
            // Just release the reservation
            walletCommandProducer.releaseFunds(
                    bill.getId(), bill.getUserId(), bill.getTotalAmount(),
                    bill.getCurrency(), "Release for failed bill: " + bill.getTransactionReference());
        } else {
            // Nothing to compensate
            bill.setCompensationCompleted(true);
            bill.setSagaStatus(BillPayment.SagaStatus.COMPENSATED);
            bill.setStatus(BillPayment.BillPaymentStatus.COMPENSATED);
            billPaymentRepository.save(bill);
        }
    }

    private BillPayment findBillOrThrow(UUID id) {
        return billPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill payment not found: " + id));
    }

    private String generateTransactionReference() {
        String ts = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BILL" + ts.substring(ts.length() - 8) + uuid;
    }

    /**
     * Return the raw entity for cross-feature read access (e.g. transaction-detail view).
     */
    @Transactional(readOnly = true)
    public BillPayment getBillPaymentEntity(UUID id) {
        return findBillOrThrow(id);
    }

    /**
     * Check existence by ID — used by WalletResponseConsumer to route SAGA events.
     */
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return billPaymentRepository.existsById(id);
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(BILL_FEE_RATE).setScale(4, RoundingMode.HALF_UP);
        return fee.compareTo(MIN_BILL_FEE) < 0 ? MIN_BILL_FEE : fee;
    }
}
