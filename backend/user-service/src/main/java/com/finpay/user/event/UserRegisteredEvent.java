package com.finpay.user.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event received from auth-service when a new user registers.
 * Used to create the full user profile in user-service.
 */
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String oauthProvider,
        String oauthProviderId,
        String profileImageUrl,
        LocalDateTime timestamp
) {
    public boolean isOAuthUser() {
        return oauthProvider != null && !oauthProvider.isBlank();
    }
}
