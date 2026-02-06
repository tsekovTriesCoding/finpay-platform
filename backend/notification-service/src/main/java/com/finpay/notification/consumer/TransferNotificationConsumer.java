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
 * Kafka consumer for money transfer notification events.
 * Listens to the transfer-notifications topic and sends notifications
 * to both sender and recipient.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferNotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper kafkaObjectMapper;

    @KafkaListener(topics = "transfer-notifications", groupId = "notification-service-group")
    public void consumeTransferNotification(String message) {
        log.info("Received transfer notification event: {}", message);

        try {
            Map<String, Object> event = kafkaObjectMapper.readValue(message, new TypeReference<>() {});
            
            String transferId = (String) event.get("transferId");
            String transactionReference = (String) event.get("transactionReference");
            String senderUserId = (String) event.get("senderUserId");
            String recipientUserId = (String) event.get("recipientUserId");
            Object amountObj = event.get("amount");
            String currency = (String) event.get("currency");
            String description = (String) event.get("description");
            String sagaStep = (String) event.get("sagaStep");

            if (senderUserId == null || recipientUserId == null) {
                log.warn("Invalid transfer notification event: missing user IDs");
                return;
            }

            BigDecimal amount = parseAmount(amountObj);

            // Send notification to both sender and recipient
            if ("SEND_NOTIFICATION".equals(sagaStep) || "COMPLETE".equals(sagaStep)) {
                notifySender(UUID.fromString(senderUserId), transactionReference, amount, currency, description);
                notifyRecipient(UUID.fromString(recipientUserId), transactionReference, amount, currency, description);
            }
            
        } catch (Exception e) {
            log.error("Error processing transfer notification event: {}", e.getMessage(), e);
        }
    }

    private BigDecimal parseAmount(Object amountObj) {
        if (amountObj instanceof Number) {
            return new BigDecimal(amountObj.toString());
        } else if (amountObj instanceof String) {
            return new BigDecimal((String) amountObj);
        }
        return BigDecimal.ZERO;
    }

    private void notifySender(UUID userId, String reference, BigDecimal amount, String currency, String description) {
        log.info("Sending transfer notification to sender: {}", userId);

        String subject = "Money Sent Successfully - FinPay";
        String content = String.format("""
            Your money transfer has been completed successfully.
            
            Transaction Reference: %s
            Amount Sent: %s %s
            Description: %s
            
            The funds have been transferred from your wallet.
            
            Best regards,
            The FinPay Team
            """,
            reference, amount, currency, description != null ? description : "N/A"
        );

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.TRANSFER_SENT,
                Notification.NotificationChannel.EMAIL,
                subject,
                content,
                null
        );

        // Also send in-app notification
        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.TRANSFER_SENT,
                Notification.NotificationChannel.IN_APP,
                "Money Sent",
                String.format("You sent %s %s successfully!", amount, currency),
                null
        );
    }

    private void notifyRecipient(UUID userId, String reference, BigDecimal amount, String currency, String description) {
        log.info("Sending transfer notification to recipient: {}", userId);

        String subject = "Money Received - FinPay";
        String content = String.format("""
            Great news! You've received money.
            
            Transaction Reference: %s
            Amount Received: %s %s
            Description: %s
            
            The funds have been added to your wallet.
            
            Best regards,
            The FinPay Team
            """,
            reference, amount, currency, description != null ? description : "N/A"
        );

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.TRANSFER_RECEIVED,
                Notification.NotificationChannel.EMAIL,
                subject,
                content,
                null
        );

        // Also send in-app notification
        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.TRANSFER_RECEIVED,
                Notification.NotificationChannel.IN_APP,
                "Money Received",
                String.format("You received %s %s!", amount, currency),
                null
        );
    }
}
