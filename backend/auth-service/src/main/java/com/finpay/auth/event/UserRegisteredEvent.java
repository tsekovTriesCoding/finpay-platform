package com.finpay.auth.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a new user registers.
 * User-service listens to this and creates the full user profile.
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
    public static UserRegisteredEvent create(UUID userId, String email, String firstName,
                                              String lastName, String phoneNumber) {
        return new UserRegisteredEvent(
                userId, email, firstName, lastName, phoneNumber,
                null, null, null, LocalDateTime.now()
        );
    }

    public static UserRegisteredEvent createOAuth(UUID userId, String email, String firstName,
                                                   String lastName, String oauthProvider,
                                                   String oauthProviderId, String profileImageUrl) {
        return new UserRegisteredEvent(
                userId, email, firstName, lastName, null,
                oauthProvider, oauthProviderId, profileImageUrl, LocalDateTime.now()
        );
    }
}
