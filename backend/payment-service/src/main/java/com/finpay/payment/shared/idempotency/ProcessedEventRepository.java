package com.finpay.payment.shared.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    @Modifying
    @Query("DELETE FROM ProcessedEvent e WHERE e.processedAt < :cutoff")
    int deleteOlderThan(LocalDateTime cutoff);
}
