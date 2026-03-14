package com.finpay.wallet.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Caches wallet read responses in Redis to reduce DB load for
 * frequent balance checks.  Cache entries have a short TTL and
 * are evicted on every write operation (deposit, withdraw, transfer).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletCacheService {

    private static final String WALLET_PREFIX = "wallet:user:";
    private static final Duration WALLET_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public WalletResponse getCachedWallet(UUID userId) {
        try {
            String json = redisTemplate.opsForValue().get(WALLET_PREFIX + userId);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, WalletResponse.class);
        } catch (Exception e) {
            log.debug("Failed to read cached wallet for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public void cacheWallet(UUID userId, WalletResponse wallet) {
        try {
            String json = objectMapper.writeValueAsString(wallet);
            redisTemplate.opsForValue().set(WALLET_PREFIX + userId, json, WALLET_TTL);
        } catch (Exception e) {
            log.debug("Failed to cache wallet for {}: {}", userId, e.getMessage());
        }
    }

    public void evictWallet(UUID userId) {
        try {
            redisTemplate.delete(WALLET_PREFIX + userId);
        } catch (Exception e) {
            log.debug("Failed to evict wallet cache for {}: {}", userId, e.getMessage());
        }
    }
}
