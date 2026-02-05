package com.finpay.auth.dto;

/**
 * Request to create a new user via user-service.
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
) {
    public static CreateUserRequest fromRegisterRequest(RegisterRequest request, String encodedPassword) {
        return new CreateUserRequest(
                request.email(),
                encodedPassword,
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                "PENDING_VERIFICATION",
                "USER",
                "LOCAL",
                null,
                null,
                false
        );
    }

    public static CreateUserRequest forOAuth2User(
            String email,
            String firstName,
            String lastName,
            String authProvider,
            String providerId,
            String profileImageUrl
    ) {
        return new CreateUserRequest(
                email,
                "", // OAuth users don't have passwords
                firstName,
                lastName,
                null,
                "ACTIVE",
                "USER",
                authProvider,
                providerId,
                profileImageUrl,
                true // OAuth emails are verified
        );
    }
}
