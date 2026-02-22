package com.finpay.user.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event received from auth-service when a user upgrades their subscription plan.
 */
public record PlanUpgradedEvent(
        UUID userId,
        String email,
        String previousPlan,
        String newPlan,
        LocalDateTime timestamp
) {}
