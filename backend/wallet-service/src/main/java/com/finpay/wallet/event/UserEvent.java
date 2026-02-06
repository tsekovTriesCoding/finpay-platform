package com.finpay.wallet.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User event received from user-service.
 * Used to react to user lifecycle events (e.g., create wallet on registration).
 */
public record UserEvent(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        EventType eventType,
        LocalDateTime timestamp
) {
    public enum EventType {
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        USER_STATUS_CHANGED,
        USER_EMAIL_VERIFIED,
        USER_PHONE_VERIFIED
    }
}
