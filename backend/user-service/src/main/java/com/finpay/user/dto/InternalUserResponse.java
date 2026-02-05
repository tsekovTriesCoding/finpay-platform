package com.finpay.user.dto;

import com.finpay.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Internal user DTO that includes password for auth-service.
 */
public record InternalUserResponse(
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
    public static InternalUserResponse fromEntity(User user) {
        return new InternalUserResponse(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getRole() != null ? user.getRole().name() : null,
                user.getAuthProvider() != null ? user.getAuthProvider().name() : null,
                user.getProviderId(),
                user.getProfileImageUrl(),
                user.getAddress(),
                user.getCity(),
                user.getCountry(),
                user.getPostalCode(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt()
        );
    }
}
