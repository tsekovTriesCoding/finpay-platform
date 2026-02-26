package com.finpay.auth.security;

import com.finpay.auth.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDto testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm-test");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "finpay-auth-service-test");

        UUID userId = UUID.randomUUID();
        testUser = new UserDto(
                userId, "john@example.com", null, "John", "Doe",
                "+1234567890", "ACTIVE", "USER", null, null, null,
                null, null, null, null, true, false, "STARTER",
                LocalDateTime.now(), LocalDateTime.now(), null
        );
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        @Test
        @DisplayName("should generate access token")
        void shouldGenerateAccessToken() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("should generate refresh token")
        void shouldGenerateRefreshToken() {
            String token = jwtService.generateRefreshToken(testUser);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("access and refresh tokens should be different")
        void tokensShouldBeDifferent() {
            String accessToken = jwtService.generateAccessToken(testUser);
            String refreshToken = jwtService.generateRefreshToken(testUser);

            assertThat(accessToken).isNotEqualTo(refreshToken);
        }
    }

    @Nested
    @DisplayName("Token Extraction")
    class TokenExtractionTests {

        @Test
        @DisplayName("should extract user ID from token")
        void shouldExtractUserId() {
            String token = jwtService.generateAccessToken(testUser);

            String extractedUserId = jwtService.extractUserId(token);

            assertThat(extractedUserId).isEqualTo(testUser.id().toString());
        }

        @Test
        @DisplayName("should extract user ID as UUID from token")
        void shouldExtractUserIdAsUUID() {
            String token = jwtService.generateAccessToken(testUser);

            UUID extractedUserId = jwtService.extractUserIdAsUUID(token);

            assertThat(extractedUserId).isEqualTo(testUser.id());
        }

        @Test
        @DisplayName("should extract email from token")
        void shouldExtractEmail() {
            String token = jwtService.generateAccessToken(testUser);

            String extractedEmail = jwtService.extractEmail(token);

            assertThat(extractedEmail).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should extract role from token")
        void shouldExtractRole() {
            String token = jwtService.generateAccessToken(testUser);

            String extractedRole = jwtService.extractRole(token);

            assertThat(extractedRole).isEqualTo("USER");
        }

        @Test
        @DisplayName("should extract expiration from token")
        void shouldExtractExpiration() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.extractExpiration(token)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("should validate valid token")
        void shouldValidateValidToken() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should validate token with user")
        void shouldValidateTokenWithUser() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
        }

        @Test
        @DisplayName("should reject token with wrong user")
        void shouldRejectTokenWithWrongUser() {
            String token = jwtService.generateAccessToken(testUser);

            UserDto wrongUser = new UserDto(
                    UUID.randomUUID(), "other@example.com", null, "Other", "User",
                    null, "ACTIVE", "USER", null, null, null,
                    null, null, null, null, true, false, "STARTER",
                    LocalDateTime.now(), LocalDateTime.now(), null
            );

            assertThat(jwtService.isTokenValid(token, wrongUser)).isFalse();
        }

        @Test
        @DisplayName("should reject expired token")
        void shouldRejectExpiredToken() {
            // Set a very short expiration
            ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L);

            String token = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("should reject malformed token")
        void shouldRejectMalformedToken() {
            assertThat(jwtService.isTokenValid("not-a-valid-token")).isFalse();
        }

        @Test
        @DisplayName("should reject empty token")
        void shouldRejectEmptyToken() {
            assertThat(jwtService.isTokenValid("")).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Expiration Config")
    class ExpirationConfigTests {

        @Test
        @DisplayName("should return access token expiration")
        void shouldReturnAccessTokenExpiration() {
            assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(900000L);
        }

        @Test
        @DisplayName("should return refresh token expiration")
        void shouldReturnRefreshTokenExpiration() {
            assertThat(jwtService.getRefreshTokenExpiration()).isEqualTo(604800000L);
        }
    }
}
