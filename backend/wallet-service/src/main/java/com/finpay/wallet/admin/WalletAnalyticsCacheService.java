package com.finpay.wallet.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Caches wallet analytics/metrics in Redis to avoid expensive
 * aggregate queries on every admin dashboard load.
 * Cache is invalidated on wallet mutations and expires after 30 seconds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletAnalyticsCacheService {

    private static final String METRICS_KEY = "wallet:analytics:metrics";
    private static final Duration METRICS_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Retrieve cached metrics, or null if not cached / expired.
     */
    public AdminWalletMetrics getCachedMetrics() {
        try {
            String json = redisTemplate.opsForValue().get(METRICS_KEY);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, AdminWalletMetrics.class);
        } catch (Exception e) {
            log.debug("Failed to read cached metrics: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Cache the computed metrics with a short TTL.
     */
    public void cacheMetrics(AdminWalletMetrics metrics) {
        try {
            String json = objectMapper.writeValueAsString(metrics);
            redisTemplate.opsForValue().set(METRICS_KEY, json, METRICS_TTL);
            log.debug("Cached wallet analytics metrics");
        } catch (Exception e) {
            log.debug("Failed to cache metrics: {}", e.getMessage());
        }
    }

    /**
     * Evict the cached metrics (called after wallet mutations).
     */
    public void evictMetrics() {
        try {
            redisTemplate.delete(METRICS_KEY);
        } catch (Exception e) {
            log.debug("Failed to evict metrics cache: {}", e.getMessage());
        }
    }
}
