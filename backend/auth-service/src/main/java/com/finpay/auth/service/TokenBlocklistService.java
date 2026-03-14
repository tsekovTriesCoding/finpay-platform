package com.finpay.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed token blocklist for instant token revocation checks.
 * When a user logs out or their tokens are revoked, the access token's JTI
 * is added here with a TTL matching the token's remaining lifetime.
 * This avoids DB lookups on every authenticated request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlocklistService {

    private static final String BLOCKLIST_PREFIX = "auth:blocklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Block a token by storing its identifier in Redis with a TTL.
     *
     * @param tokenId   unique token identifier (JWT subject + issued-at, or token hash)
     * @param ttl       time until the token would naturally expire
     */
    public void blockToken(String tokenId, Duration ttl) {
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        if (ttl.isPositive()) {
            try {
                redisTemplate.opsForValue().set(BLOCKLIST_PREFIX + tokenId, "revoked", ttl);
                log.debug("Blocked token: {} with TTL: {}", tokenId, ttl);
            } catch (Exception e) {
                log.warn("Failed to block token in Redis: {}", e.getMessage());
            }
        }
    }

    /**
     * Check if a token has been revoked.
     * Returns false (fail-open) if Redis is unavailable - tokens have short TTL,
     * so the window of exposure is bounded by the access token lifetime.
     */
    public boolean isBlocked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLOCKLIST_PREFIX + tokenId));
        } catch (Exception e) {
            log.warn("Redis unavailable for blocklist check, failing open: {}", e.getMessage());
            return false;
        }
    }
}
