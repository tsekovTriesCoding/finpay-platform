package com.finpay.user.dto;

import com.finpay.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        User.UserStatus status,
        User.UserRole role,
        String profileImageUrl,
        String address,
        String city,
        String country,
        String postalCode,
        boolean emailVerified,
        boolean phoneVerified,
        User.AccountPlan plan,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getRole(),
                user.getProfileImageUrl(),
                user.getAddress(),
                user.getCity(),
                user.getCountry(),
                user.getPostalCode(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getPlan(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt()
        );
    }
}
