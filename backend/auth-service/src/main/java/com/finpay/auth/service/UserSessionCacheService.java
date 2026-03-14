package com.finpay.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.auth.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Caches user session data in Redis to avoid DB lookups on every
 * /auth/me call. The cached profile is invalidated on logout, status
 * changes, and profile updates (via Kafka events).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionCacheService {

    private static final String SESSION_PREFIX = "auth:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Cache a user profile for the given user ID.
     */
    public void cacheUserSession(UUID userId, UserDto userDto) {
        try {
            String json = objectMapper.writeValueAsString(userDto);
            redisTemplate.opsForValue().set(SESSION_PREFIX + userId, json, SESSION_TTL);
            log.debug("Cached session for user: {}", userId);
        } catch (Exception e) {
            log.debug("Failed to cache user session for {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Retrieve a cached user profile, or null if not cached.
     */
    public UserDto getCachedSession(UUID userId) {
        try {
            String json = redisTemplate.opsForValue().get(SESSION_PREFIX + userId);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, UserDto.class);
        } catch (Exception e) {
            log.debug("Failed to read cached session for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Remove the cached session (on logout, status change, profile update).
     */
    public void evictSession(UUID userId) {
        try {
            redisTemplate.delete(SESSION_PREFIX + userId);
            log.debug("Evicted session cache for user: {}", userId);
        } catch (Exception e) {
            log.debug("Failed to evict session cache for {}: {}", userId, e.getMessage());
        }
    }
}
