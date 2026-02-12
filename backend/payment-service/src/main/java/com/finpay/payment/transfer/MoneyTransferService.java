package com.finpay.payment.transfer;

import com.finpay.payment.transfer.dto.MoneyTransferRequest;
import com.finpay.payment.transfer.dto.MoneyTransferResponse;
import com.finpay.payment.transfer.event.TransferSagaEvent;
import com.finpay.payment.shared.exception.ResourceNotFoundException;
import com.finpay.payment.shared.exception.TransferException;
import com.finpay.payment.shared.kafka.WalletCommandProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Money Transfer Service implementing SAGA Choreography pattern for distributed transactions.
 * 
 * Fully event-driven architecture using Kafka:
 * - No REST calls to other services
 * - All wallet operations handled via Kafka commands/events
 * - Wallets are created automatically when users register (via user-events)
 * 
 * SAGA Steps (executed via Kafka events):
 * 1. RESERVE_FUNDS - Reserve funds in sender's wallet (wallet-service)
 * 2. DEDUCT_FUNDS - Deduct funds from sender's wallet (wallet-service)
 * 3. CREDIT_FUNDS - Credit funds to recipient's wallet (wallet-service)
 * 4. SEND_NOTIFICATION - Notify both parties (notification-service)
 * 5. COMPLETE - Mark transfer as complete
 * 
 * Compensation (if any step fails) is handled automatically in reverse order.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MoneyTransferService {

    private final MoneyTransferRepository transferRepository;
    private final WalletCommandProducer walletCommandProducer;
    private final TransferSagaEventProducer sagaEventProducer;

    /**
     * Initiate a money transfer using the SAGA Choreography pattern.
     * Creates the transfer record and starts the SAGA via Kafka.
     * Returns immediately with PROCESSING status - saga executes asynchronously.
     * 
     * Wallet validation happens asynchronously - if wallet doesn't exist or has
     * insufficient funds, the saga will fail and compensation will be triggered.
     */
    public MoneyTransferResponse initiateTransfer(UUID senderUserId, MoneyTransferRequest request) {
        log.info("Initiating transfer from {} to {} for amount {} {}",
                senderUserId, request.recipientUserId(), request.amount(), request.currency());

        // Validate sender and recipient are different
        if (senderUserId.equals(request.recipientUserId())) {
            throw new TransferException("Cannot transfer money to yourself");
        }

        // Validate amount is positive
        if (request.amount().signum() <= 0) {
            throw new TransferException("Transfer amount must be positive");
        }

        // Generate transaction reference
        String transactionReference = generateTransactionReference();

        // Create transfer record (wallet IDs will be populated by wallet-service responses)
        MoneyTransfer transfer = MoneyTransfer.builder()
                .transactionReference(transactionReference)
                .senderUserId(senderUserId)
                .recipientUserId(request.recipientUserId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .description(request.description())
                .transferType(MoneyTransfer.TransferType.SEND)
                .status(MoneyTransfer.TransferStatus.PROCESSING)
                .sagaStatus(MoneyTransfer.SagaStatus.STARTED)
                .fundsReserved(false)
                .fundsDeducted(false)
                .fundsCredit(false)
                .notificationSent(false)
                .compensationRequired(false)
                .compensationCompleted(false)
                .build();

        MoneyTransfer savedTransfer = transferRepository.save(transfer);
        log.info("Transfer created with ID: {} and reference: {}", savedTransfer.getId(), transactionReference);

        // Publish saga initiate event for tracking/debugging
        TransferSagaEvent sagaEvent = TransferSagaEvent.initiate(
                savedTransfer.getId(),
                transactionReference,
                senderUserId,
                null,  // Wallet IDs not known yet - will be resolved by wallet-service
                request.recipientUserId(),
                null,
                request.amount(),
                request.currency(),
                request.description()
        );
        sagaEventProducer.sendSagaEvent(sagaEvent);

        // Start SAGA Step 1: Reserve funds via Kafka
        log.info("Starting SAGA via Kafka - Step 1: Reserve funds for transfer {}", savedTransfer.getId());
        walletCommandProducer.reserveFunds(
                savedTransfer.getId(),  // correlationId for saga tracking
                senderUserId,
                request.amount(),
                request.currency(),
                "Reserve funds for transfer: " + transactionReference
        );

        return MoneyTransferResponse.fromEntity(savedTransfer);
    }

    /**
     * Get transfer by ID.
     */
    @Transactional(readOnly = true)
    public MoneyTransferResponse getTransferById(UUID transferId) {
        MoneyTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));
        return MoneyTransferResponse.fromEntity(transfer);
    }

    /**
     * Get transfer by transaction reference.
     */
    @Transactional(readOnly = true)
    public MoneyTransferResponse getTransferByReference(String transactionReference) {
        MoneyTransfer transfer = transferRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found with reference: " + transactionReference));
        return MoneyTransferResponse.fromEntity(transfer);
    }

    /**
     * Get all transfers for a user (as sender or recipient).
     */
    @Transactional(readOnly = true)
    public Page<MoneyTransferResponse> getTransfersForUser(UUID userId, Pageable pageable) {
        return transferRepository.findByUserIdAsParticipant(userId, pageable)
                .map(MoneyTransferResponse::fromEntity);
    }

    /**
     * Return the raw entity for cross-feature read access (e.g. transaction-detail view).
     */
    @Transactional(readOnly = true)
    public MoneyTransfer getTransferEntity(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));
    }

    /**
     * Look up a transfer by ID — returns Optional for routing in WalletResponseConsumer.
     */
    @Transactional(readOnly = true)
    public Optional<MoneyTransfer> findOptionalById(UUID id) {
        return transferRepository.findById(id);
    }

    // ======================== Request-triggered transfers ========================

    /**
     * Create a transfer record for an approved money request.
     * Persists the record, publishes a SAGA trace event, and starts
     * SAGA Step 1 (reserve funds) — all in one atomic operation.
     *
     * @return the persisted MoneyTransfer so the caller can read its ID
     */
    public MoneyTransfer createTransferForRequest(UUID senderUserId, UUID recipientUserId,
                                                   BigDecimal amount, String currency,
                                                   String description, UUID sourceRequestId) {
        String txRef = generateTransactionReference();

        MoneyTransfer transfer = MoneyTransfer.builder()
                .transactionReference(txRef)
                .senderUserId(senderUserId)
                .recipientUserId(recipientUserId)
                .amount(amount)
                .currency(currency)
                .description(description)
                .transferType(MoneyTransfer.TransferType.REQUEST_PAYMENT)
                .sourceRequestId(sourceRequestId)
                .status(MoneyTransfer.TransferStatus.PROCESSING)
                .sagaStatus(MoneyTransfer.SagaStatus.STARTED)
                .fundsReserved(false)
                .fundsDeducted(false)
                .fundsCredit(false)
                .notificationSent(false)
                .compensationRequired(false)
                .compensationCompleted(false)
                .build();

        transfer = transferRepository.save(transfer);
        log.info("Created transfer {} (ref={}) for money-request {}", transfer.getId(), txRef, sourceRequestId);

        // Publish SAGA trace event
        sagaEventProducer.sendSagaEvent(TransferSagaEvent.initiate(
                transfer.getId(), txRef,
                senderUserId, null,
                recipientUserId, null,
                amount, currency, description
        ));

        // SAGA Step 1: Reserve funds from the payer
        log.info("Starting request-payment SAGA – Step 1: Reserve funds from payer {} via transfer {}",
                senderUserId, transfer.getId());
        walletCommandProducer.reserveFunds(
                transfer.getId(), senderUserId,
                amount, currency,
                "Reserve funds for payment request: " + txRef
        );

        return transfer;
    }

    // ======================== SAGA step handlers ========================
    // Called by WalletResponseConsumer to advance or compensate the transfer SAGA.

    /**
     * SAGA Step 1 completed — funds reserved → send DEDUCT command.
     */
    public void handleFundsReserved(UUID transferId, UUID walletId) {
        log.info("SAGA Step 1 completed: Funds reserved for transfer {}", transferId);
        MoneyTransfer transfer = getTransferEntity(transferId);

        transfer.setFundsReserved(true);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.FUNDS_RESERVED);
        if (walletId != null) {
            transfer.setSenderWalletId(walletId);
        }
        transferRepository.save(transfer);

        walletCommandProducer.deductFunds(
                transfer.getId(), transfer.getSenderUserId(),
                transfer.getAmount(), transfer.getCurrency(),
                "Transfer deduction: " + transfer.getTransactionReference()
        );
    }

    /**
     * SAGA Step 2 completed — funds deducted → send CREDIT command.
     */
    public void handleFundsDeducted(UUID transferId) {
        log.info("SAGA Step 2 completed: Funds deducted for transfer {}", transferId);
        MoneyTransfer transfer = getTransferEntity(transferId);

        transfer.setFundsDeducted(true);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.FUNDS_DEDUCTED);
        transferRepository.save(transfer);

        walletCommandProducer.creditFunds(
                transfer.getId(), transfer.getRecipientUserId(),
                transfer.getAmount(), transfer.getCurrency(),
                "Transfer credit from user " + transfer.getSenderUserId()
        );
    }

    /**
     * SAGA Step 3 completed — funds credited → send notification and complete.
     *
     * @return result containing linked-request info for cross-feature coordination
     */
    public TransferSagaStepResult handleFundsCredited(UUID transferId, UUID walletId) {
        log.info("SAGA Step 3 completed: Funds credited for transfer {}", transferId);
        MoneyTransfer transfer = getTransferEntity(transferId);

        transfer.setFundsCredit(true);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.FUNDS_CREDITED);
        if (walletId != null) {
            transfer.setRecipientWalletId(walletId);
        }
        transferRepository.save(transfer);

        // Send completion notification via Kafka (transfer-notifications topic).
        // This is the single notification path for all completed transfers,
        // including request-payments - no duplicate from money-request-events.
        TransferSagaEvent notificationEvent = new TransferSagaEvent(
                transfer.getId(), transfer.getTransactionReference(),
                transfer.getSenderUserId(), transfer.getSenderWalletId(),
                transfer.getRecipientUserId(), transfer.getRecipientWalletId(),
                transfer.getAmount(), transfer.getCurrency(), transfer.getDescription(),
                TransferSagaEvent.SagaStep.SEND_NOTIFICATION, TransferSagaEvent.SagaAction.EXECUTE,
                null, LocalDateTime.now()
        );
        sagaEventProducer.sendNotificationEvent(notificationEvent);

        // Mark transfer as complete
        transfer.setNotificationSent(true);
        transfer.setStatus(MoneyTransfer.TransferStatus.COMPLETED);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPLETED);
        transfer.setCompletedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        log.info("SAGA completed successfully for transfer {}", transfer.getId());
        return TransferSagaStepResult.of(transfer);
    }

    /**
     * Handle SAGA failure — marks the transfer as FAILED and starts compensation.
     *
     * @return result containing linked-request info for cross-feature coordination
     */
    public TransferSagaStepResult handleSagaFailure(UUID transferId, String failureReason) {
        log.error("Wallet operation failed for transfer {}: {}", transferId, failureReason);
        MoneyTransfer transfer = getTransferEntity(transferId);

        transfer.setFailureReason(failureReason);
        transfer.setStatus(MoneyTransfer.TransferStatus.FAILED);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.FAILED);
        transfer.setCompensationRequired(true);
        transfer.setFailedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        startCompensation(transfer);
        return TransferSagaStepResult.of(transfer);
    }

    // ======================== Compensation handlers ========================

    /** Compensation: funds released after failed saga. */
    public void handleFundsReleased(UUID transferId) {
        log.info("Compensation: Funds released for transfer {}", transferId);
        MoneyTransfer transfer = getTransferEntity(transferId);
        transfer.setCompensationCompleted(true);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATED);
        transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATED);
        transferRepository.save(transfer);
    }

    /** Compensation: credit reversed → continue chain (reverse deduction or release). */
    public void handleCreditReversed(UUID transferId) {
        log.info("Compensation: Credit reversed for transfer {}", transferId);
        MoneyTransfer transfer = getTransferEntity(transferId);

        if (transfer.isFundsDeducted()) {
            walletCommandProducer.reverseDeduction(
                    transfer.getId(), transfer.getSenderUserId(),
                    transfer.getAmount(), transfer.getCurrency(),
                    "Reversal of deduction for failed transfer: " + transfer.getTransactionReference()
            );
        } else if (transfer.isFundsReserved()) {
            walletCommandProducer.releaseFunds(
                    transfer.getId(), transfer.getSenderUserId(),
                    transfer.getAmount(), transfer.getCurrency(),
                    "Release funds for failed transfer: " + transfer.getTransactionReference()
            );
        } else {
            transfer.setCompensationCompleted(true);
            transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATED);
            transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATED);
            transferRepository.save(transfer);
        }
    }

    /** Compensation: deduction reversed → release reservation. */
    public void handleDeductionReversed(UUID transferId) {
        log.info("Compensation: Deduction reversed for transfer {}", transferId);
        MoneyTransfer transfer = getTransferEntity(transferId);

        if (transfer.isFundsReserved()) {
            walletCommandProducer.releaseFunds(
                    transfer.getId(), transfer.getSenderUserId(),
                    transfer.getAmount(), transfer.getCurrency(),
                    "Release funds after rollback: " + transfer.getTransactionReference()
            );
        } else {
            transfer.setCompensationCompleted(true);
            transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATED);
            transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATED);
            transferRepository.save(transfer);
        }
    }

    private void startCompensation(MoneyTransfer transfer) {
        log.warn("Starting compensation for failed transfer {}", transfer.getId());

        transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATING);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATING);
        transferRepository.save(transfer);

        if (transfer.isFundsCredit()) {
            walletCommandProducer.reverseCredit(
                    transfer.getId(), transfer.getRecipientUserId(),
                    transfer.getAmount(), transfer.getCurrency(),
                    "Reversal for failed transfer: " + transfer.getTransactionReference()
            );
        } else if (transfer.isFundsDeducted()) {
            walletCommandProducer.reverseDeduction(
                    transfer.getId(), transfer.getSenderUserId(),
                    transfer.getAmount(), transfer.getCurrency(),
                    "Reversal of deduction for failed transfer: " + transfer.getTransactionReference()
            );
        } else if (transfer.isFundsReserved()) {
            walletCommandProducer.releaseFunds(
                    transfer.getId(), transfer.getSenderUserId(),
                    transfer.getAmount(), transfer.getCurrency(),
                    "Release funds for failed transfer: " + transfer.getTransactionReference()
            );
        } else {
            transfer.setCompensationCompleted(true);
            transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATED);
            transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATED);
            transferRepository.save(transfer);
        }
    }

    private String generateTransactionReference() {
        return "TRF-" + System.currentTimeMillis() + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
