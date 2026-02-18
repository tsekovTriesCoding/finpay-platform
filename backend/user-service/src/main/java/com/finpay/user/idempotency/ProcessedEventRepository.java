package com.finpay.user.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    /**
     * Cleanup: delete processed events older than the given cutoff.
     * Keeps the table from growing unboundedly.
     */
    @Modifying
    @Query("DELETE FROM ProcessedEvent e WHERE e.processedAt < :cutoff")
    int deleteOlderThan(LocalDateTime cutoff);
}
