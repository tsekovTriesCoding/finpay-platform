package com.finpay.user.idempotency;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks events that have been successfully processed by this service's consumers.
 * Used to guarantee idempotent message processing - if an event is redelivered
 * (e.g. due to Kafka offset not committed, or outbox publisher crash-after-send),
 * the consumer detects the duplicate via this table and skips reprocessing.
 */
@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_processed_consumer_group", columnList = "consumerGroup"),
        @Index(name = "idx_processed_at", columnList = "processedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    /**
     * The idempotency key - typically the outbox event UUID,
     * passed as the X-Idempotency-Key Kafka header.
     */
    @Id
    @Column(length = 64)
    private String eventId;

    /**
     * Which consumer group processed this event (for auditing/debugging).
     */
    @Column(nullable = false, length = 100)
    private String consumerGroup;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}
