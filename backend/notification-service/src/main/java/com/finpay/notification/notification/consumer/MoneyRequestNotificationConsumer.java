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

/**
 * Kafka consumer for money-request lifecycle events.
 * Listens to the money-request-events topic and sends appropriate notifications
 * for each stage: created, approved, declined, cancelled, expired, completed, failed.
 *
 * Configured with non-blocking retries and DLT:
 * - 4 attempts with exponential backoff (1s, 2s, 4s)
 * - Failed messages go to money-request-events-dlt
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MoneyRequestNotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper kafkaObjectMapper;
    private final IdempotentConsumerService idempotentConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = "money-request-events", groupId = "notification-service-group")
    public void consumeMoneyRequestEvent(String message,
                                         @Header(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws Exception {
        if (idempotentConsumer.isDuplicate(idempotencyKey)) {
            log.info("Duplicate money request event detected, skipping: idempotencyKey={}", idempotencyKey);
            return;
        }

        log.info("Received money request event: {}", message);

        Map<String, Object> event = kafkaObjectMapper.readValue(message, new TypeReference<>() {});

        String requestId = (String) event.get("requestId");
        String requestReference = (String) event.get("requestReference");
        String requesterUserId = (String) event.get("requesterUserId");
        String payerUserId = (String) event.get("payerUserId");
        Object amountObj = event.get("amount");
        String currency = (String) event.get("currency");
        String description = (String) event.get("description");
        String eventType = (String) event.get("eventType");
        String failureReason = (String) event.get("failureReason");

        if (requesterUserId == null || payerUserId == null || eventType == null) {
            log.warn("Invalid money request event: missing required fields");
            return;
        }

        BigDecimal amount = parseAmount(amountObj);
        UUID requester = UUID.fromString(requesterUserId);
        UUID payer = UUID.fromString(payerUserId);

        switch (eventType) {
            case "REQUEST_CREATED" -> notifyRequestCreated(payer, requester, requestReference, amount, currency, description);
            case "REQUEST_APPROVED" -> notifyRequestApproved(requester, payer, requestReference, amount, currency);
            case "REQUEST_DECLINED" -> notifyRequestDeclined(requester, payer, requestReference, amount, currency);
            case "REQUEST_CANCELLED" -> notifyRequestCancelled(payer, requester, requestReference, amount, currency);
            case "REQUEST_EXPIRED" -> notifyRequestExpired(requester, payer, requestReference, amount, currency);
            // REQUEST_COMPLETED notifications are handled by TransferNotificationConsumer
            // via the transfer-notifications topic to avoid duplicate messages.
            case "REQUEST_COMPLETED" -> log.info("Skipping REQUEST_COMPLETED notification (handled by TransferNotificationConsumer)");
            case "REQUEST_FAILED" -> notifyRequestFailed(requester, payer, requestReference, amount, currency, failureReason);
            default -> log.warn("Unknown money request event type: {}", eventType);
        }

        idempotentConsumer.markProcessed(idempotencyKey, "money-request-notification-consumer");
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        log.error("DLT: Failed to process money request event after all retries. Topic: {}, Key: {}, Value: {}, Error: {}",
                topic, record.key(), record.value(), errorMessage);
    }

    private void notifyRequestCreated(UUID payerUserId, UUID requesterUserId,
                                       String reference, BigDecimal amount,
                                       String currency, String description) {
        log.info("Notifying payer {} about incoming money request", payerUserId);

        String subject = "Money Request Received - FinPay";
        String content = String.format("""
            You've received a money request.
            
            Request Reference: %s
            Amount Requested: %s %s
            Note: %s
            
            Please open FinPay to approve or decline this request.
            
            Best regards,
            The FinPay Team
            """,
                reference, amount, currency, description != null ? description : "N/A"
        );

        notificationService.createAndSendNotification(
                payerUserId,
                Notification.NotificationType.PAYMENT_INITIATED,
                Notification.NotificationChannel.EMAIL,
                subject, content, null
        );

        notificationService.createAndSendNotification(
                payerUserId,
                Notification.NotificationType.PAYMENT_INITIATED,
                Notification.NotificationChannel.IN_APP,
                "Money Request",
                String.format("Someone requested %s %s from you", amount, currency),
                null
        );
    }

    private void notifyRequestApproved(UUID requesterUserId, UUID payerUserId,
                                        String reference, BigDecimal amount, String currency) {
        notificationService.createAndSendNotification(
                requesterUserId,
                Notification.NotificationType.PAYMENT_INITIATED,
                Notification.NotificationChannel.IN_APP,
                "Request Approved",
                String.format("Your request for %s %s was approved! Payment is being processed.", amount, currency),
                null
        );
    }

    private void notifyRequestDeclined(UUID requesterUserId, UUID payerUserId,
                                        String reference, BigDecimal amount, String currency) {
        String subject = "Money Request Declined - FinPay";
        String content = String.format("""
            Your money request has been declined.
            
            Request Reference: %s
            Amount Requested: %s %s
            
            You can send a new request or contact the recipient.
            
            Best regards,
            The FinPay Team
            """,
                reference, amount, currency
        );

        notificationService.createAndSendNotification(
                requesterUserId,
                Notification.NotificationType.PAYMENT_FAILED,
                Notification.NotificationChannel.EMAIL,
                subject, content, null
        );

        notificationService.createAndSendNotification(
                requesterUserId,
                Notification.NotificationType.PAYMENT_FAILED,
                Notification.NotificationChannel.IN_APP,
                "Request Declined",
                String.format("Your request for %s %s was declined.", amount, currency),
                null
        );
    }

    private void notifyRequestCancelled(UUID payerUserId, UUID requesterUserId,
                                         String reference, BigDecimal amount, String currency) {
        notificationService.createAndSendNotification(
                payerUserId,
                Notification.NotificationType.SYSTEM,
                Notification.NotificationChannel.IN_APP,
                "Request Cancelled",
                String.format("A money request for %s %s was cancelled by the requester.", amount, currency),
                null
        );
    }

    private void notifyRequestExpired(UUID requesterUserId, UUID payerUserId,
                                       String reference, BigDecimal amount, String currency) {
        notificationService.createAndSendNotification(
                requesterUserId,
                Notification.NotificationType.SYSTEM,
                Notification.NotificationChannel.IN_APP,
                "Request Expired",
                String.format("Your money request for %s %s has expired.", amount, currency),
                null
        );

        notificationService.createAndSendNotification(
                payerUserId,
                Notification.NotificationType.SYSTEM,
                Notification.NotificationChannel.IN_APP,
                "Request Expired",
                String.format("A money request for %s %s has expired.", amount, currency),
                null
        );
    }

    private void notifyRequestFailed(UUID requesterUserId, UUID payerUserId,
                                      String reference, BigDecimal amount,
                                      String currency, String failureReason) {
        String reason = failureReason != null ? failureReason : "An error occurred during processing";

        notificationService.createAndSendNotification(
                requesterUserId,
                Notification.NotificationType.PAYMENT_FAILED,
                Notification.NotificationChannel.IN_APP,
                "Request Payment Failed",
                String.format("Payment for your %s %s request failed: %s", amount, currency, reason),
                null
        );

        notificationService.createAndSendNotification(
                payerUserId,
                Notification.NotificationType.PAYMENT_FAILED,
                Notification.NotificationChannel.IN_APP,
                "Payment Failed",
                String.format("Your payment of %s %s for a request failed: %s", amount, currency, reason),
                null
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
}
