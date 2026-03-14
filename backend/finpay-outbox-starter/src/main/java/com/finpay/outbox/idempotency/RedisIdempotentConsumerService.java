package com.finpay.outbox.idempotency;

import com.finpay.outbox.OutboxProperties;
import com.finpay.outbox.entity.ProcessedEvent;
import com.finpay.outbox.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Redis-accelerated idempotent consumer.
 *
 * Duplicate checks hit Redis first (sub-millisecond) and only fall
 * through to the database when the key is missing from cache.
 * Writes go to both Redis (with TTL) and the database (durable
 * source of truth).
 *
 * This is a drop-in replacement for {@link IdempotentConsumerService}
 * and is auto-configured when Redis is on the classpath.
 */
@Slf4j
public class RedisIdempotentConsumerService extends IdempotentConsumerService {

    private static final String IDEMPOTENCY_PREFIX = "outbox:processed:";

    private final StringRedisTemplate redisTemplate;
    private final ProcessedEventRepository repository;
    private final OutboxProperties properties;

    public RedisIdempotentConsumerService(ProcessedEventRepository repository,
                                          OutboxProperties properties,
                                          StringRedisTemplate redisTemplate) {
        super(repository, properties);
        this.repository = repository;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isDuplicate(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        // Fast path: check Redis
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(IDEMPOTENCY_PREFIX + eventId))) {
                return true;
            }
        } catch (Exception e) {
            log.debug("Redis unavailable for idempotency check, falling back to DB: {}", e.getMessage());
        }
        // Slow path: check DB (and backfill Redis if found)
        boolean exists = repository.existsById(eventId);
        if (exists) {
            backfillRedis(eventId);
        }
        return exists;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void markProcessed(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        // Write to DB first (source of truth)
        if (repository.existsById(eventId)) {
            return;
        }
        try {
            repository.saveAndFlush(ProcessedEvent.builder()
                    .eventId(eventId)
                    .consumerGroup(consumerGroup)
                    .processedAt(LocalDateTime.now())
                    .build());
            log.debug("Marked event as processed: eventId={}, consumer={}", eventId, consumerGroup);
        } catch (DataIntegrityViolationException e) {
            log.debug("Concurrent processed event insert (safe to ignore): eventId={}", eventId);
        }

        // Write to Redis with TTL matching retention
        try {
            Duration ttl = Duration.ofDays(properties.getIdempotency().getRetentionDays());
            redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + eventId, consumerGroup, ttl);
        } catch (Exception e) {
            log.debug("Failed to cache processed event in Redis: {}", e.getMessage());
        }
    }

    private void backfillRedis(String eventId) {
        try {
            Duration ttl = Duration.ofDays(properties.getIdempotency().getRetentionDays());
            redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + eventId, "backfill", ttl);
        } catch (Exception e) {
            log.debug("Failed to backfill Redis for eventId={}: {}", eventId, e.getMessage());
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${finpay.idempotency.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanup() {
        super.cleanup();
    }
}
