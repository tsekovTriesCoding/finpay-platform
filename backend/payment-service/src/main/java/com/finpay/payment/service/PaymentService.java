package com.finpay.payment.service;

import com.finpay.payment.dto.PaymentRequest;
import com.finpay.payment.dto.PaymentResponse;
import com.finpay.payment.entity.Payment;
import com.finpay.payment.event.PaymentEvent;
import com.finpay.payment.exception.PaymentException;
import com.finpay.payment.exception.ResourceNotFoundException;
import com.finpay.payment.mapper.PaymentMapper;
import com.finpay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentMapper paymentMapper;

    private static final BigDecimal PROCESSING_FEE_RATE = new BigDecimal("0.015"); // 1.5%
    private static final BigDecimal MIN_PROCESSING_FEE = new BigDecimal("0.50");

    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating payment for user: {} amount: {} {}", 
                request.userId(), request.amount(), request.currency());

        // Generate unique transaction reference
        String transactionReference = generateTransactionReference();

        // Calculate processing fee
        BigDecimal processingFee = calculateProcessingFee(request.amount());
        BigDecimal totalAmount = request.amount().add(processingFee);

        // Use mapper for basic field mapping
        Payment payment = paymentMapper.toEntity(request);
        // Set computed fields
        payment.setTransactionReference(transactionReference);
        payment.setCurrency(request.currency().toUpperCase());
        payment.setProcessingFee(processingFee);
        payment.setTotalAmount(totalAmount);

        // Mask card details if present
        if (request.cardNumber() != null && request.cardNumber().length() >= 4) {
            payment.setCardLastFourDigits(request.cardNumber().substring(request.cardNumber().length() - 4));
            payment.setCardType(detectCardType(request.cardNumber()));
        }

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment initiated with ID: {} and reference: {}", savedPayment.getId(), transactionReference);

        // Publish payment initiated event
        publishPaymentEvent(savedPayment, PaymentEvent.EventType.PAYMENT_INITIATED);

        // Start async processing
        processPaymentAsync(savedPayment.getId());

        return paymentMapper.toResponse(savedPayment);
    }

    private void processPaymentAsync(UUID paymentId) {
        // In a real implementation, this would be handled by a separate processor
        // For now, we'll simulate async processing
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate processing time
                processPayment(paymentId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Payment processing interrupted for ID: {}", paymentId);
            }
        }).start();
    }

    public PaymentResponse processPayment(UUID paymentId) {
        log.info("Processing payment with ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new PaymentException("Payment is not in PENDING status");
        }

        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        publishPaymentEvent(payment, PaymentEvent.EventType.PAYMENT_PROCESSING);

        try {
            // Simulate payment gateway integration
            boolean success = simulatePaymentGateway(payment);

            if (success) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setProcessedAt(LocalDateTime.now());
                payment.setGatewayReference("GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                payment.setGatewayResponse("SUCCESS");
                
                Payment completedPayment = paymentRepository.save(payment);
                log.info("Payment completed successfully: {}", paymentId);
                publishPaymentEvent(completedPayment, PaymentEvent.EventType.PAYMENT_COMPLETED);
                
                return paymentMapper.toResponse(completedPayment);
            } else {
                throw new PaymentException("Payment gateway declined the transaction");
            }
        } catch (Exception e) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            payment.setGatewayResponse("FAILED");
            
            Payment failedPayment = paymentRepository.save(payment);
            log.error("Payment failed: {} - {}", paymentId, e.getMessage());
            publishPaymentEvent(failedPayment, PaymentEvent.EventType.PAYMENT_FAILED);
            
            return paymentMapper.toResponse(failedPayment);
        }
    }

    private boolean simulatePaymentGateway(Payment payment) {
        // Simulate 95% success rate
        return Math.random() < 0.95;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id) {
        log.debug("Fetching payment with ID: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + id));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String transactionReference) {
        log.debug("Fetching payment with reference: {}", transactionReference);
        Payment payment = paymentRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + transactionReference));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(UUID userId) {
        log.debug("Fetching payments for user: {}", userId);
        return paymentRepository.findByUserId(userId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByUserId(UUID userId, Pageable pageable) {
        log.debug("Fetching paginated payments for user: {}", userId);
        return paymentRepository.findByUserId(userId, pageable)
                .map(paymentMapper::toResponse);
    }

    public PaymentResponse cancelPayment(UUID paymentId) {
        log.info("Cancelling payment with ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new PaymentException("Only pending payments can be cancelled");
        }

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        Payment cancelledPayment = paymentRepository.save(payment);
        
        publishPaymentEvent(cancelledPayment, PaymentEvent.EventType.PAYMENT_CANCELLED);
        
        return paymentMapper.toResponse(cancelledPayment);
    }

    public PaymentResponse refundPayment(UUID paymentId) {
        log.info("Refunding payment with ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new PaymentException("Only completed payments can be refunded");
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        Payment refundedPayment = paymentRepository.save(payment);
        
        publishPaymentEvent(refundedPayment, PaymentEvent.EventType.PAYMENT_REFUNDED);
        
        return paymentMapper.toResponse(refundedPayment);
    }

    private String generateTransactionReference() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "FP" + timestamp.substring(timestamp.length() - 8) + uuid;
    }

    private BigDecimal calculateProcessingFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(PROCESSING_FEE_RATE).setScale(4, RoundingMode.HALF_UP);
        return fee.compareTo(MIN_PROCESSING_FEE) < 0 ? MIN_PROCESSING_FEE : fee;
    }

    private String detectCardType(String cardNumber) {
        if (cardNumber.startsWith("4")) {
            return "VISA";
        } else if (cardNumber.startsWith("5")) {
            return "MASTERCARD";
        } else if (cardNumber.startsWith("37") || cardNumber.startsWith("34")) {
            return "AMEX";
        } else if (cardNumber.startsWith("6")) {
            return "DISCOVER";
        }
        return "UNKNOWN";
    }

    private void publishPaymentEvent(Payment payment, PaymentEvent.EventType eventType) {
        PaymentEvent event = paymentMapper.toEvent(payment, eventType);
        paymentEventProducer.sendPaymentEvent(event);
    }
}
