package com.finpay.outbox.idempotency;

import com.finpay.outbox.OutboxProperties;
import com.finpay.outbox.entity.ProcessedEvent;
import com.finpay.outbox.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Provides idempotent message processing for Kafka consumers.
 *
 * <p>Before processing a message the consumer calls
 * {@link #isDuplicate(String)}; after successful processing it calls
 * {@link #markProcessed(String, String)}.  A scheduled cleanup task
 * removes stale records based on {@link OutboxProperties.Idempotency}.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class IdempotentConsumerService {

    private final ProcessedEventRepository repository;
    private final OutboxProperties properties;

    /**
     * Returns {@code true} if this event has already been processed.
     */
    public boolean isDuplicate(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return repository.existsById(eventId);
    }

    /**
     * Record that the event has been processed.  Safe under concurrency —
     * a {@link DataIntegrityViolationException} on a duplicate insert is
     * silently swallowed.
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
     * Periodic cleanup of old processed-event records.
     */
    @Scheduled(fixedDelayString = "${finpay.idempotency.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanup() {
        int retentionDays = properties.getIdempotency().getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = repository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Idempotency cleanup: deleted {} old processed events", deleted);
        }
    }
}
