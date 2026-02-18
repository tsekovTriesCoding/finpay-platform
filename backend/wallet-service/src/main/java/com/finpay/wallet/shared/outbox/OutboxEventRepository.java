package com.finpay.wallet.shared.outbox;

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

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents(Pageable pageable);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'SENT' AND e.processedAt < :cutoff")
    int deleteSentEventsBefore(LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PENDING' WHERE e.status = 'FAILED' AND e.retryCount < e.maxRetries")
    int requeueFailedEvents();
}
