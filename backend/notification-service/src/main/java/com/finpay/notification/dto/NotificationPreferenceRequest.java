package com.finpay.notification.dto;

public record NotificationPreferenceRequest(
        boolean emailEnabled,
        boolean smsEnabled,
        boolean pushEnabled,
        boolean inAppEnabled,
        boolean paymentNotifications,
        boolean securityNotifications,
        boolean promotionalNotifications,
        boolean systemNotifications
) {}
