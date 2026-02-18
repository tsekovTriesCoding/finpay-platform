package com.finpay.notification.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.notification.notification.Notification;
import com.finpay.notification.notification.NotificationService;
import com.finpay.notification.shared.idempotency.IdempotentConsumerService;
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

/**
 * Consumes bill-payment-events from Kafka and creates user notifications.
 *
 * Configured with non-blocking retries and DLT:
 * - 4 attempts with exponential backoff (1s, 2s, 4s)
 * - Failed messages go to bill-payment-events-dlt
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillPaymentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper kafkaObjectMapper;
    private final IdempotentConsumerService idempotentConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = "bill-payment-events", groupId = "notification-service-group")
    public void consumeBillPaymentEvent(String message,
                                        @Header(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws Exception {
        if (idempotentConsumer.isDuplicate(idempotencyKey)) {
            log.info("Duplicate bill payment event detected, skipping: idempotencyKey={}", idempotencyKey);
            return;
        }

        log.info("Received bill payment event: {}", message);

        Map<String, Object> event = kafkaObjectMapper.readValue(message, new TypeReference<>() {});

        String eventType = (String) event.get("eventType");
        String userId = (String) event.get("userId");
        String transactionReference = (String) event.get("transactionReference");
        String billerName = (String) event.get("billerName");
        String category = (String) event.get("category");
        Object amountObj = event.get("amount");
        String currency = (String) event.get("currency");
        String failureReason = (String) event.get("failureReason");

        if (userId == null || eventType == null) {
            log.warn("Invalid bill payment event: missing required fields");
            return;
        }

        BigDecimal amount = parseAmount(amountObj);

        switch (eventType) {
            case "BILL_PAYMENT_INITIATED" ->
                    handleInitiated(UUID.fromString(userId), transactionReference, billerName, category, amount, currency);
            case "BILL_PAYMENT_COMPLETED" ->
                    handleCompleted(UUID.fromString(userId), transactionReference, billerName, category, amount, currency);
            case "BILL_PAYMENT_FAILED" ->
                    handleFailed(UUID.fromString(userId), transactionReference, billerName, category, amount, currency, failureReason);
            default -> log.debug("Ignoring bill payment event type: {}", eventType);
        }

        idempotentConsumer.markProcessed(idempotencyKey, "bill-payment-event-consumer");
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        log.error("DLT: Failed to process bill payment event after all retries. Topic: {}, Key: {}, Value: {}, Error: {}",
                topic, record.key(), record.value(), errorMessage);
    }

    private void handleInitiated(UUID userId, String ref, String biller, String category,
                                  BigDecimal amount, String currency) {
        log.info("Processing BILL_PAYMENT_INITIATED for user: {}", userId);

        String subject = "Bill Payment Initiated - FinPay";
        String content = String.format("""
            Your bill payment has been initiated.
            
            Biller: %s (%s)
            Reference: %s
            Amount: %s %s
            Status: Processing
            
            We'll notify you once the payment is confirmed.
            
            Best regards,
            The FinPay Team
            """, biller, category, ref, formatAmount(amount), currency);

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.BILL_PAYMENT_INITIATED,
                Notification.NotificationChannel.IN_APP,
                subject, content, null
        );
    }

    private void handleCompleted(UUID userId, String ref, String biller, String category,
                                  BigDecimal amount, String currency) {
        log.info("Processing BILL_PAYMENT_COMPLETED for user: {}", userId);

        String subject = "Bill Payment Successful - FinPay";
        String content = String.format("""
            Your bill payment has been completed successfully!
            
            Biller: %s (%s)
            Reference: %s
            Amount: %s %s
            Status: Completed
            
            Thank you for using FinPay!
            
            Best regards,
            The FinPay Team
            """, biller, category, ref, formatAmount(amount), currency);

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.BILL_PAYMENT_COMPLETED,
                Notification.NotificationChannel.EMAIL,
                subject, content, null
        );
    }

    private void handleFailed(UUID userId, String ref, String biller, String category,
                               BigDecimal amount, String currency, String reason) {
        log.info("Processing BILL_PAYMENT_FAILED for user: {}", userId);

        String subject = "Bill Payment Failed - FinPay";
        String content = String.format("""
            Your bill payment could not be processed.
            
            Biller: %s (%s)
            Reference: %s
            Amount: %s %s
            Status: Failed
            Reason: %s
            
            Please try again or contact support.
            
            Best regards,
            The FinPay Team
            """, biller, category, ref, formatAmount(amount), currency,
                reason != null ? reason : "Unknown error");

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.BILL_PAYMENT_FAILED,
                Notification.NotificationChannel.EMAIL,
                subject, content, null
        );
    }

    private BigDecimal parseAmount(Object amountObj) {
        if (amountObj instanceof Number) {
            return new BigDecimal(amountObj.toString());
        } else if (amountObj instanceof String) {
            return new BigDecimal((String) amountObj);
        }
        return BigDecimal.ZERO;
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }
}
