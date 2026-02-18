package com.finpay.auth.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional Outbox entity.
 * 
 * Instead of publishing events directly to Kafka, services persist events
 * into this table within the same database transaction as the business
 * operation.  A background poller ({@link OutboxPublisher}) then reads
 * PENDING rows, publishes them to Kafka, and marks them as SENT.

 * This guarantees that an event is published if and only if the database
 * transaction commits, achieving at-least-once delivery semantics.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregateType, aggregateId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Logical aggregate that produced the event (e.g. "UserCredential"). */
    @Column(nullable = false)
    private String aggregateType;

    /** Business identifier of the aggregate instance (e.g. user-id). */
    @Column(nullable = false)
    private String aggregateId;

    /** Discriminator for the event payload (e.g. "USER_REGISTERED"). */
    @Column(nullable = false)
    private String eventType;

    /** Kafka topic the event should be published to. */
    @Column(nullable = false)
    private String topic;

    /** Kafka message key (used for partitioning). */
    @Column(name = "event_key")
    private String eventKey;

    /** Serialised JSON payload of the event. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private int retryCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int maxRetries = 5;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime processedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public enum OutboxStatus {
        PENDING,
        SENT,
        FAILED
    }
}
