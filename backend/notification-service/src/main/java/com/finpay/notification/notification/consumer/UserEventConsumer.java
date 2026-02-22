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

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper kafkaObjectMapper;
    private final IdempotentConsumerService idempotentConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = "user-events", groupId = "notification-service-group")
    public void consumeUserEvent(String message,
                                 @Header(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws Exception {
        if (idempotentConsumer.isDuplicate(idempotencyKey)) {
            log.info("Duplicate user event detected, skipping: idempotencyKey={}", idempotencyKey);
            return;
        }

        log.info("Received user event: {}", message);

        Map<String, Object> event = kafkaObjectMapper.readValue(message, new TypeReference<>() {});

        String eventType = (String) event.get("eventType");
        String userId = (String) event.get("userId");
        String email = (String) event.get("email");
        String firstName = (String) event.get("firstName");

        if (userId == null || eventType == null) {
            log.warn("Invalid user event received: missing required fields");
            return;
        }

        switch (eventType) {
            case "USER_CREATED" -> handleUserCreated(UUID.fromString(userId), email, firstName);
            case "USER_EMAIL_VERIFIED" -> handleEmailVerified(UUID.fromString(userId), email, firstName);
            case "USER_STATUS_CHANGED" -> handleStatusChanged(UUID.fromString(userId), email, firstName);
            case "USER_UPDATED" -> handleUserUpdated(UUID.fromString(userId), firstName);
            case "PLAN_UPGRADED" -> handlePlanUpgraded(
                    UUID.fromString(userId), email, firstName, (String) event.get("plan"));
            default -> log.debug("Ignoring user event type: {}", eventType);
        }

        idempotentConsumer.markProcessed(idempotencyKey, "user-event-notification-consumer");
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        log.error("DLT: Failed to process user event after all retries. Topic: {}, Key: {}, Value: {}, Error: {}",
                topic, record.key(), record.value(), errorMessage);
    }

    private void handleUserCreated(UUID userId, String email, String firstName) {
        log.info("Processing USER_CREATED event for user: {}", userId);

        String subject = "Welcome to FinPay!";
        String content = String.format("""
            Hi %s,
            
            Welcome to FinPay! Your account has been created successfully.
            
            Please verify your email address to start using all features of our platform.
            
            If you have any questions, feel free to contact our support team.
            
            Best regards,
            The FinPay Team
            """, firstName != null ? firstName : "there");

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.USER_REGISTRATION,
                Notification.NotificationChannel.EMAIL,
                subject,
                content,
                email
        );
    }

    private void handleEmailVerified(UUID userId, String email, String firstName) {
        log.info("Processing USER_EMAIL_VERIFIED event for user: {}", userId);

        String subject = "Email Verified - FinPay";
        String content = String.format("""
            Hi %s,
            
            Your email address has been verified successfully!
            
            You now have full access to all FinPay features. Start making secure payments today.
            
            Best regards,
            The FinPay Team
            """, firstName != null ? firstName : "there");

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.EMAIL_VERIFICATION,
                Notification.NotificationChannel.EMAIL,
                subject,
                content,
                email
        );
    }

    private void handleStatusChanged(UUID userId, String email, String firstName) {
        log.info("Processing USER_STATUS_CHANGED event for user: {}", userId);

        String subject = "Account Status Update - FinPay";
        String content = String.format("""
            Hi %s,
            
            Your account status has been updated.
            
            If you did not request this change, please contact our support team immediately.
            
            Best regards,
            The FinPay Team
            """, firstName != null ? firstName : "there");

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.ACCOUNT_UPDATE,
                Notification.NotificationChannel.EMAIL,
                subject,
                content,
                email
        );
    }

    private void handleUserUpdated(UUID userId, String firstName) {
        log.info("Processing USER_UPDATED event for user: {}", userId);

        String subject = "Profile Updated - FinPay";
        String content = String.format("""
            Hi %s,
            
            Your profile has been updated successfully.
            
            If you did not make this change, please contact our support team immediately.
            
            Best regards,
            The FinPay Team
            """, firstName != null ? firstName : "there");

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.ACCOUNT_UPDATE,
                Notification.NotificationChannel.EMAIL,
                subject,
                content,
                null
        );
    }

    private void handlePlanUpgraded(UUID userId, String email, String firstName, String newPlan) {
        log.info("Processing PLAN_UPGRADED event for user: {} to plan: {}", userId, newPlan);

        String planLabel = newPlan != null ? switch (newPlan.toUpperCase()) {
            case "PRO" -> "Pro";
            case "ENTERPRISE" -> "Enterprise";
            default -> newPlan;
        } : "new";

        // 1. In-app notification (pushed in real time via WebSocket)
        String inAppSubject = "Plan Upgraded to " + planLabel + " \uD83C\uDF89";
        String inAppContent = "Your account has been upgraded to the " + planLabel
                + " plan. Enjoy your new limits and features!";

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.PLAN_UPGRADED,
                Notification.NotificationChannel.IN_APP,
                inAppSubject,
                inAppContent,
                null
        );

        // 2. Confirmation email
        String emailSubject = "Plan Upgraded to " + planLabel + " - FinPay";
        String emailContent = String.format("""
            Hi %s,
            
            Great news! Your FinPay account has been upgraded to the %s plan.
            
            Your new plan features are now active, including upgraded transaction
            limits and additional capabilities.
            
            You can review your plan details in your account settings.
            
            If you did not make this change, please contact our support team immediately.
            
            Best regards,
            The FinPay Team
            """, firstName != null ? firstName : "there", planLabel);

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.PLAN_UPGRADED,
                Notification.NotificationChannel.EMAIL,
                emailSubject,
                emailContent,
                email
        );
    }
}
