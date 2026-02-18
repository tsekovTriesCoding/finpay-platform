package com.finpay.payment.shared.idempotency;

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
 * 
 * Critical for the SAGA choreography - WalletResponseConsumer must not
 * advance the transfer SAGA twice on duplicate wallet-events delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotentConsumerService {

    private final ProcessedEventRepository repository;

    public boolean isDuplicate(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return repository.existsById(eventId);
    }

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
