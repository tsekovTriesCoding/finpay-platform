package com.finpay.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserDto user
) {
    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserDto user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }

    /**
     * Returns a copy of this response with tokens removed.
     * Used when tokens are sent via HTTP-only cookies instead of response body.
     */
    public AuthResponse withoutTokens() {
        return new AuthResponse(null, null, this.tokenType, this.expiresIn, this.user);
    }
}
