package com.finpay.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finpay.auth.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionCacheService Unit Tests")
class UserSessionCacheServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private UserSessionCacheService cacheService;

    private UUID userId;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userDto = new UserDto(
                userId, "john@example.com", null,
                "John", "Doe", "+1234567890", "ACTIVE", "USER",
                null, null, null, null, null, null, null,
                true, false, "STARTER", null, null, null
        );
    }

    @Nested
    @DisplayName("cacheUserSession")
    class CacheUserSessionTests {

        @Test
        @DisplayName("should store user session in Redis with TTL")
        void shouldStoreSessionWithTtl() throws Exception {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String expectedJson = objectMapper.writeValueAsString(userDto);

            cacheService.cacheUserSession(userId, userDto);

            verify(valueOperations).set(
                    eq("auth:session:" + userId),
                    eq(expectedJson),
                    eq(Duration.ofMinutes(15))
            );
        }
    }

    @Nested
    @DisplayName("getCachedSession")
    class GetCachedSessionTests {

        @Test
        @DisplayName("should return cached user when present")
        void shouldReturnCachedUser() throws Exception {
            String json = objectMapper.writeValueAsString(userDto);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("auth:session:" + userId)).thenReturn(json);

            UserDto result = cacheService.getCachedSession(userId);

            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo("john@example.com");
            assertThat(result.firstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("should return null when cache miss")
        void shouldReturnNullOnCacheMiss() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("auth:session:" + userId)).thenReturn(null);

            assertThat(cacheService.getCachedSession(userId)).isNull();
        }
    }

    @Nested
    @DisplayName("evictSession")
    class EvictSessionTests {

        @Test
        @DisplayName("should delete session from Redis")
        void shouldDeleteSession() {
            cacheService.evictSession(userId);

            verify(redisTemplate).delete("auth:session:" + userId);
        }
    }
}
