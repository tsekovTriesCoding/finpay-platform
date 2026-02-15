package com.finpay.notification.preference.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationPreferenceEvent(
        UUID userId,
        boolean emailEnabled,
        boolean smsEnabled,
        boolean pushEnabled,
        boolean inAppEnabled,
        boolean paymentNotifications,
        boolean securityNotifications,
        boolean promotionalNotifications,
        boolean systemNotifications,
        String eventType,
        LocalDateTime timestamp
) {
    public enum EventType {
        PREFERENCES_UPDATED,
        PREFERENCES_CREATED
    }
}
