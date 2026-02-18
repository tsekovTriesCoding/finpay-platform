package com.finpay.user.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Provides idempotent message processing for Kafka consumers.

 * Usage pattern:
 * <pre>
 * if (idempotentConsumer.isDuplicate(idempotencyKey)) {
 *     return; // skip
 * }
 * // ... process message ...
 * idempotentConsumer.markProcessed(idempotencyKey, "my-consumer");
 * </pre>

 * For @Transactional consumers, markProcessed joins the caller's transaction
 * (Propagation.REQUIRED) so the idempotency mark commits/rolls back with
 * the business logic atomically.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotentConsumerService {

    private final ProcessedEventRepository repository;

    /**
     * Check if an event has already been processed.
     *
     * @param eventId the idempotency key (outbox event UUID from Kafka header)
     * @return true if this event was already processed (duplicate)
     */
    public boolean isDuplicate(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return repository.existsById(eventId);
    }

    /**
     * Mark an event as successfully processed.

     * Uses Propagation.REQUIRED so it joins an existing transaction if one
     * is active (e.g. @Transactional consumer), ensuring the mark commits
     * atomically with the business logic. If no transaction is active,
     * it creates its own.

     * Safe against concurrent inserts - catches DataIntegrityViolationException
     * from the unique constraint on eventId.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void markProcessed(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
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
    }

    /**
     * Cleanup: delete processed events older than 14 days.
     * Runs hourly to keep the table from growing unboundedly.
     */
    @Scheduled(fixedDelayString = "${idempotency.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(14);
        int deleted = repository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Idempotency cleanup: deleted {} old processed events", deleted);
        }
    }
}
