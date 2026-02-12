package com.finpay.notification.repository;

import com.finpay.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserId(UUID userId);

    List<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(UUID userId, Notification.NotificationChannel channel);

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndChannel(UUID userId, Notification.NotificationChannel channel, Pageable pageable);

    List<Notification> findByUserIdAndStatus(UUID userId, Notification.NotificationStatus status);

    List<Notification> findByUserIdAndReadAtIsNull(UUID userId);

    List<Notification> findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(UUID userId, Notification.NotificationChannel channel);

    long countByUserIdAndChannelAndReadAtIsNull(UUID userId, Notification.NotificationChannel channel);

    List<Notification> findByStatus(Notification.NotificationStatus status);

    List<Notification> findByStatusAndRetryCountLessThan(Notification.NotificationStatus status, int maxRetries);

    List<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByUserIdAndReadAtIsNull(UUID userId);
}
