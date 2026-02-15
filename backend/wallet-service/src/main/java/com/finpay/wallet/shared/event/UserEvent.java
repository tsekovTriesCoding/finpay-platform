package com.finpay.wallet.shared.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserEvent(
        UUID userId, String email, String firstName, String lastName,
        EventType eventType, LocalDateTime timestamp
) {
    public enum EventType {
        USER_CREATED, USER_UPDATED, USER_DELETED,
        USER_STATUS_CHANGED, USER_EMAIL_VERIFIED, USER_PHONE_VERIFIED
    }
}
