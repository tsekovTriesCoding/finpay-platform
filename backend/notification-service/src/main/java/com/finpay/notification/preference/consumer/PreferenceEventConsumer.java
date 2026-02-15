package com.finpay.notification.preference.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.notification.notification.Notification;
import com.finpay.notification.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreferenceEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper kafkaObjectMapper;

    @KafkaListener(topics = "notification-preference-events", groupId = "notification-preference-audit-group")
    public void consumePreferenceEvent(String message) {
        log.info("Received notification preference event: {}", message);

        try {
            Map<String, Object> event = kafkaObjectMapper.readValue(message, new TypeReference<>() {});

            String eventType = (String) event.get("eventType");
            String userId = (String) event.get("userId");

            if (userId == null || eventType == null) {
                log.warn("Invalid preference event received: missing required fields");
                return;
            }

            switch (eventType) {
                case "PREFERENCES_UPDATED" -> handlePreferencesUpdated(UUID.fromString(userId), event);
                case "PREFERENCES_CREATED" -> log.info("Default preferences created for user: {}", userId);
                default -> log.debug("Ignoring preference event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing notification preference event: {}", e.getMessage(), e);
        }
    }

    private void handlePreferencesUpdated(UUID userId, Map<String, Object> event) {
        log.info("Processing PREFERENCES_UPDATED event for user: {}", userId);

        // Send an in-app notification confirming the preference change
        String subject = "Notification Preferences Updated";
        String content = "Your notification preferences have been updated successfully. " +
                "Changes will take effect immediately for all future notifications.";

        notificationService.createAndSendNotification(
                userId,
                Notification.NotificationType.ACCOUNT_UPDATE,
                Notification.NotificationChannel.IN_APP,
                subject,
                content,
                null
        );
    }
}
