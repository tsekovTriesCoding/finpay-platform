package com.finpay.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing user data received from user-service.
 */
public record UserDto(
        UUID id,
        String email,
        String password,
        String firstName,
        String lastName,
        String phoneNumber,
        String status,
        String role,
        String authProvider,
        String providerId,
        String profileImageUrl,
        String address,
        String city,
        String country,
        String postalCode,
        boolean emailVerified,
        boolean phoneVerified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt
) {
    /**
     * Returns a UserDto without sensitive password field for responses.
     */
    public UserDto withoutPassword() {
        return new UserDto(
                id, email, null, firstName, lastName, phoneNumber,
                status, role, authProvider, providerId, profileImageUrl,
                address, city, country, postalCode, emailVerified,
                phoneVerified, createdAt, updatedAt, lastLoginAt
        );
    }
}
