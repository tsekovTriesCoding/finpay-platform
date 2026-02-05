package com.finpay.user.dto;

/**
 * DTO for creating users from auth-service.
 * Contains all fields needed for user creation including password.
 */
public record CreateUserRequest(
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
        boolean emailVerified
) {}
