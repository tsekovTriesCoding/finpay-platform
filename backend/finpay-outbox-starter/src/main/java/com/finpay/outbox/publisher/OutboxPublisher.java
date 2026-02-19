package com.finpay.outbox.publisher;

import com.finpay.outbox.OutboxProperties;
import com.finpay.outbox.entity.OutboxEvent;
import com.finpay.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Polling publisher for the Transactional Outbox Pattern.
 *
 * Picks up {@code PENDING} outbox events, publishes them to Kafka with
 * an {@code X-Idempotency-Key} header, and marks them as {@code SENT}.
 * A separate scheduled method cleans up old events and re-queues failed
 * ones that still have retries left.
 *
 * Tuning knobs live in {@link OutboxProperties}.
 */
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "${finpay.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPendingEvents() {
        int batchSize = properties.getOutbox().getBatchSize();
        List<OutboxEvent> pending = outboxRepository.findPendingEvents(PageRequest.of(0, batchSize));

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Outbox publisher found {} pending events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        event.getTopic(), null, event.getEventKey(), event.getPayload());
                record.headers().add("X-Idempotency-Key",
                        event.getId().toString().getBytes(StandardCharsets.UTF_8));
                kafkaTemplate.send(record)
                        .get();

                event.setStatus(OutboxEvent.OutboxStatus.SENT);
                event.setProcessedAt(LocalDateTime.now());
                log.info("Outbox event published: id={}, topic={}, key={}",
                        event.getId(), event.getTopic(), event.getEventKey());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());

                if (event.getRetryCount() >= event.getMaxRetries()) {
                    event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                    log.error("Outbox event permanently failed after {} retries: id={}, topic={}",
                            event.getRetryCount(), event.getId(), event.getTopic(), e);
                } else {
                    log.warn("Outbox event publish failed (retry {}/{}): id={}, topic={}",
                            event.getRetryCount(), event.getMaxRetries(),
                            event.getId(), event.getTopic(), e);
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${finpay.outbox.cleanup-interval-ms:600000}")
    @Transactional
    public void cleanupAndRetry() {
        int retentionDays = properties.getOutbox().getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        int deleted = outboxRepository.deleteSentEventsBefore(cutoff);
        if (deleted > 0) {
            log.info("Outbox cleanup: deleted {} old SENT events", deleted);
        }

        int requeued = outboxRepository.requeueFailedEvents();
        if (requeued > 0) {
            log.info("Outbox cleanup: re-queued {} FAILED events for retry", requeued);
        }
    }
}
