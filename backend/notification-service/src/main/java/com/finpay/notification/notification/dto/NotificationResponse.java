package com.finpay.notification.notification.dto;

import com.finpay.notification.notification.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        Notification.NotificationType type,
        Notification.NotificationChannel channel,
        String subject,
        String content,
        String recipient,
        Notification.NotificationStatus status,
        String errorMessage,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}
