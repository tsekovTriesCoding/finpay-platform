package com.finpay.notification.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.notification.notification.Notification;
import com.finpay.notification.notification.NotificationService;
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
 * Kafka consumer for money transfer notification events.
 * Listens to the transfer-notifications topic and sends notifications
 * to both sender and recipient.
 *
 * Configured with non-blocking retries and DLT:
 * - 4 attempts with exponential backoff (1s, 2s, 4s)
 * - Failed messages go to transfer-notifications-dlt
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferNotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper kafkaObjectMapper;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = "transfer-notifications", groupId = "notification-service-group")
    public void consumeTransferNotification(String message) throws Exception {
        log.info("Received transfer notification event: {}", message);

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
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        log.error("DLT: Failed to process transfer notification after all retries. Topic: {}, Key: {}, Value: {}, Error: {}",
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
