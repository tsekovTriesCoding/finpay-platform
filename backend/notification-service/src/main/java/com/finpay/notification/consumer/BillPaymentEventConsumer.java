package com.finpay.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.notification.entity.Notification;
import com.finpay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes bill-payment-events from Kafka and creates user notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillPaymentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper kafkaObjectMapper;

    @KafkaListener(topics = "bill-payment-events", groupId = "notification-service-group")
    public void consumeBillPaymentEvent(String message) {
        log.info("Received bill payment event: {}", message);

        try {
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
        } catch (Exception e) {
            log.error("Error processing bill payment event: {}", e.getMessage(), e);
        }
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
