package com.finpay.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlocklistService Unit Tests")
class TokenBlocklistServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlocklistService blocklistService;

    @Nested
    @DisplayName("blockToken")
    class BlockTokenTests {

        @Test
        @DisplayName("should store token in Redis with TTL")
        void shouldStoreTokenWithTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            Duration ttl = Duration.ofMinutes(30);
            blocklistService.blockToken("user123:1234567890", ttl);

            verify(valueOperations).set("auth:blocklist:user123:1234567890", "revoked", ttl);
        }

        @Test
        @DisplayName("should not store if TTL is zero or negative")
        void shouldNotStoreIfTtlNonPositive() {
            blocklistService.blockToken("user123:1234567890", Duration.ZERO);

            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("should not store if tokenId is null")
        void shouldNotStoreIfTokenIdNull() {
            blocklistService.blockToken(null, Duration.ofMinutes(5));

            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("should not store if tokenId is blank")
        void shouldNotStoreIfTokenIdBlank() {
            blocklistService.blockToken("  ", Duration.ofMinutes(5));

            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("isBlocked")
    class IsBlockedTests {

        @Test
        @DisplayName("should return true if token exists in Redis")
        void shouldReturnTrueIfTokenExists() {
            when(redisTemplate.hasKey("auth:blocklist:user123:1234567890")).thenReturn(true);

            assertThat(blocklistService.isBlocked("user123:1234567890")).isTrue();
        }

        @Test
        @DisplayName("should return false if token does not exist in Redis")
        void shouldReturnFalseIfTokenMissing() {
            when(redisTemplate.hasKey("auth:blocklist:user123:1234567890")).thenReturn(false);

            assertThat(blocklistService.isBlocked("user123:1234567890")).isFalse();
        }

        @Test
        @DisplayName("should return false if tokenId is null")
        void shouldReturnFalseIfTokenIdNull() {
            assertThat(blocklistService.isBlocked(null)).isFalse();
        }

        @Test
        @DisplayName("should return false if tokenId is blank")
        void shouldReturnFalseIfTokenIdBlank() {
            assertThat(blocklistService.isBlocked("  ")).isFalse();
        }
    }
}
