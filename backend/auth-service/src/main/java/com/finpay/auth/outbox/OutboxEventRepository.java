package com.finpay.auth.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Fetch the oldest PENDING events, limited by {@code pageable}.
     * Uses {@code ORDER BY createdAt ASC} so events are published in the
     * order they were created - a requirement of the Outbox Pattern.
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents(Pageable pageable);

    /**
     * Bulk-delete events that were successfully sent before a given cutoff,
     * keeping the outbox table compact.
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'SENT' AND e.processedAt < :cutoff")
    int deleteSentEventsBefore(LocalDateTime cutoff);

    /**
     * Re-queue FAILED events whose retry count is still below maxRetries.
     * Called periodically so transient failures are retried automatically.
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PENDING' WHERE e.status = 'FAILED' AND e.retryCount < e.maxRetries")
    int requeueFailedEvents();
}
