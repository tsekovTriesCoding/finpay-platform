package com.finpay.outbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional Outbox entity.
 *
 * Events are persisted in the same database transaction as the business
 * operation.  A background poller ({@link com.finpay.outbox.publisher.OutboxPublisher})
 * publishes them to Kafka and marks them as {@code SENT}, guaranteeing
 * at-least-once delivery without two-phase commits.
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

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key")
    private String eventKey;

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
