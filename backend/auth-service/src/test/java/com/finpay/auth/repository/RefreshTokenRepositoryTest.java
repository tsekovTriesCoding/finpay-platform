package com.finpay.auth.repository;

import com.finpay.auth.entity.RefreshToken;
import com.finpay.auth.testconfig.TestMySQLContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMySQLContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository Data JPA Tests")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID userId;
    private RefreshToken testToken;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        userId = UUID.randomUUID();
        testToken = RefreshToken.builder()
                .token("test-refresh-token-" + UUID.randomUUID())
                .userId(userId)
                .userEmail("john@example.com")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    @Nested
    @DisplayName("Find by Token")
    class FindByTokenTests {

        @Test
        @DisplayName("should find refresh token by token value")
        void shouldFindByToken() {
            RefreshToken saved = repository.save(testToken);

            Optional<RefreshToken> found = repository.findByToken(testToken.getToken());

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(userId);
            assertThat(found.get().getUserEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should return empty for non-existent token")
        void shouldReturnEmptyForNonExistentToken() {
            Optional<RefreshToken> found = repository.findByToken("non-existent-token");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Revoke tokens")
    class RevokeTests {

        @Test
        @DisplayName("should revoke all tokens for a user")
        void shouldRevokeAllTokensForUser() {
            // Create multiple tokens for the same user
            RefreshToken token1 = RefreshToken.builder()
                    .token("token-1-" + UUID.randomUUID())
                    .userId(userId)
                    .userEmail("john@example.com")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build();
            RefreshToken token2 = RefreshToken.builder()
                    .token("token-2-" + UUID.randomUUID())
                    .userId(userId)
                    .userEmail("john@example.com")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build();

            repository.save(token1);
            repository.save(token2);

            repository.revokeAllByUserId(userId);
            entityManager.flush();
            entityManager.getEntityManager().clear();

            // Verify all tokens are revoked
            Optional<RefreshToken> found1 = repository.findByToken(token1.getToken());
            Optional<RefreshToken> found2 = repository.findByToken(token2.getToken());

            assertThat(found1).isPresent();
            assertThat(found1.get().isRevoked()).isTrue();
            assertThat(found2).isPresent();
            assertThat(found2.get().isRevoked()).isTrue();
        }

        @Test
        @DisplayName("should not revoke tokens of other users")
        void shouldNotRevokeOtherUsersTokens() {
            UUID otherUserId = UUID.randomUUID();
            RefreshToken otherToken = RefreshToken.builder()
                    .token("other-token-" + UUID.randomUUID())
                    .userId(otherUserId)
                    .userEmail("other@example.com")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build();

            repository.save(testToken);
            repository.save(otherToken);

            repository.revokeAllByUserId(userId);

            // Other user's token should still be valid
            Optional<RefreshToken> found = repository.findByToken(otherToken.getToken());
            assertThat(found).isPresent();
            assertThat(found.get().isRevoked()).isFalse();
        }
    }

    @Nested
    @DisplayName("Delete expired tokens")
    class DeleteExpiredTests {

        @Test
        @DisplayName("should delete expired tokens")
        void shouldDeleteExpiredTokens() {
            RefreshToken expiredToken = RefreshToken.builder()
                    .token("expired-token-" + UUID.randomUUID())
                    .userId(userId)
                    .userEmail("john@example.com")
                    .expiryDate(LocalDateTime.now().minusDays(1))
                    .revoked(false)
                    .build();

            repository.save(testToken);     // valid
            repository.save(expiredToken);  // expired

            repository.deleteExpiredTokens(LocalDateTime.now());

            assertThat(repository.findByToken(testToken.getToken())).isPresent();
            assertThat(repository.findByToken(expiredToken.getToken())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Token validity")
    class TokenValidityTests {

        @Test
        @DisplayName("should report valid token")
        void shouldReportValidToken() {
            RefreshToken saved = repository.save(testToken);
            Optional<RefreshToken> found = repository.findByToken(testToken.getToken());

            assertThat(found).isPresent();
            assertThat(found.get().isValid()).isTrue();
            assertThat(found.get().isExpired()).isFalse();
        }

        @Test
        @DisplayName("should report revoked token as invalid")
        void shouldReportRevokedTokenAsInvalid() {
            testToken.setRevoked(true);
            repository.save(testToken);

            Optional<RefreshToken> found = repository.findByToken(testToken.getToken());

            assertThat(found).isPresent();
            assertThat(found.get().isValid()).isFalse();
        }

        @Test
        @DisplayName("should report expired token as invalid")
        void shouldReportExpiredTokenAsInvalid() {
            testToken.setExpiryDate(LocalDateTime.now().minusDays(1));
            repository.save(testToken);

            Optional<RefreshToken> found = repository.findByToken(testToken.getToken());

            assertThat(found).isPresent();
            assertThat(found.get().isValid()).isFalse();
            assertThat(found.get().isExpired()).isTrue();
        }
    }
}
