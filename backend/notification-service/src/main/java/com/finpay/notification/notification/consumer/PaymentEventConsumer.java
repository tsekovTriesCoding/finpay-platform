package com.finpay.notification.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.notification.notification.Notification;
import com.finpay.notification.notification.NotificationService;
import com.finpay.outbox.idempotency.IdempotentConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper kafkaObjectMapper;
    private final IdempotentConsumerService idempotentConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = "payment-events", groupId = "notification-service-group")
    public void consumePaymentEvent(String message,
                                    @Header(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws Exception {
        if (idempotentConsumer.isDuplicate(idempotencyKey)) {
            log.info("Duplicate payment event detected, skipping: idempotencyKey={}", idempotencyKey);
            return;
        }

        log.info("Received payment event: {}", message);

        Map<String, Object> event = kafkaObjectMapper.readValue(message, new TypeReference<>() {});

        String eventType = (String) event.get("eventType");
        String userId = (String) event.get("userId");
        String transactionReference = (String) event.get("transactionReference");
        Object amountObj = event.get("amount");
        String currency = (String) event.get("currency");
        String status = (String) event.get("status");
        String failureReason = (String) event.get("failureReason");

        if (userId == null || eventType == null) {
            log.warn("Invalid payment event received: missing required fields");
            return;
        }

        BigDecimal amount = parseAmount(amountObj);

            switch (eventType) {
                case "PAYMENT_INITIATED" -> handlePaymentInitiated(UUID.fromString(userId), transactionReference, amount, currency);
                case "PAYMENT_COMPLETED" -> handlePaymentCompleted(UUID.fromString(userId), transactionReference, amount, currency);
                case "PAYMENT_FAILED" -> handlePaymentFailed(UUID.fromString(userId), transactionReference, amount, currency, failureReason);
                case "PAYMENT_REFUNDED" -> handlePaymentRefunded(UUID.fromString(userId), transactionReference, amount, currency);
                default -> log.debug("Ignoring payment event type: {}", eventType);
            }

        idempotentConsumer.markProcessed(idempotencyKey, "payment-event-consumer");
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        log.error("DLT: Failed to process payment event after all retries. Topic: {}, Key: {}, Value: {}, Error: {}",
                topic, record.key(), record.value(), errorMessage);
    }

    private BigDecimal parseAmount(Object amountObj) {
        if (amountObj instanceof Number) {
            return new BigDecimal(amountObj.toString());
        } else if (amountObj instanceof String) {
            return new BigDecimal((String) amountObj);
        }
        return BigDecimal.ZERO;
    }

    private void handlePaymentInitiated(UUID userId, String reference, BigDecimal amount, String currency) {
        log.info("Processing PAYMENT_INITIATED event for user: {}", userId);

        String subject = "Payment Initiated - FinPay";
        String content = String.format("""
            Your payment has been initiated.
            
            Transaction Reference: %s
            Amount: %s %s
            Status: Processing
            
            We'll notify you once the payment is complete.
            
            Best regards,
            The FinPay Team
            """, reference, formatAmount(amount), currency);

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.PAYMENT_INITIATED,
                Notification.NotificationChannel.IN_APP,
                subject,
                content,
                null
        );
    }

    private void handlePaymentCompleted(UUID userId, String reference, BigDecimal amount, String currency) {
        log.info("Processing PAYMENT_COMPLETED event for user: {}", userId);

        String subject = "Payment Successful - FinPay";
        String content = String.format("""
            Great news! Your payment has been completed successfully.
            
            Transaction Reference: %s
            Amount: %s %s
            Status: Completed
            
            Thank you for using FinPay!
            
            Best regards,
            The FinPay Team
            """, reference, formatAmount(amount), currency);

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.PAYMENT_COMPLETED,
                Notification.NotificationChannel.EMAIL,
                subject,
                content,
                null
        );
    }

    private void handlePaymentFailed(UUID userId, String reference, BigDecimal amount, String currency, String reason) {
        log.info("Processing PAYMENT_FAILED event for user: {}", userId);

        String subject = "Payment Failed - FinPay";
        String content = String.format("""
            Unfortunately, your payment could not be processed.
            
            Transaction Reference: %s
            Amount: %s %s
            Status: Failed
            Reason: %s
            
            Please try again or contact our support team if the issue persists.
            
            Best regards,
            The FinPay Team
            """, reference, formatAmount(amount), currency, reason != null ? reason : "Unknown error");

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.PAYMENT_FAILED,
                Notification.NotificationChannel.EMAIL,
                subject,
                content,
                null
        );
    }

    private void handlePaymentRefunded(UUID userId, String reference, BigDecimal amount, String currency) {
        log.info("Processing PAYMENT_REFUNDED event for user: {}", userId);

        String subject = "Payment Refunded - FinPay";
        String content = String.format("""
            Your payment has been refunded.
            
            Transaction Reference: %s
            Refund Amount: %s %s
            Status: Refunded
            
            The refund should appear in your account within 5-10 business days.
            
            Best regards,
            The FinPay Team
            """, reference, formatAmount(amount), currency);

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.PAYMENT_REFUNDED,
                Notification.NotificationChannel.EMAIL,
                subject,
                content,
                null
        );
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }
}
