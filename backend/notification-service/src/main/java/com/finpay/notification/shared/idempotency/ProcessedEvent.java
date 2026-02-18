package com.finpay.notification.shared.idempotency;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks events that have been successfully processed by this service's consumers.
 * Prevents duplicate notification delivery when the outbox pattern delivers at-least-once.
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

    @Id
    @Column(length = 64)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String consumerGroup;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}
