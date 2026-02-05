package com.finpay.notification.dto;

import com.finpay.notification.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NotificationRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Notification type is required")
        Notification.NotificationType type,

        @NotNull(message = "Notification channel is required")
        Notification.NotificationChannel channel,

        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "Content is required")
        String content,

        String recipient,

        String metadata
) {}
