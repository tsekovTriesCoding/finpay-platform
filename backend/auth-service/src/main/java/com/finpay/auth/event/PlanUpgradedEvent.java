package com.finpay.auth.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a user upgrades their subscription plan.
 * User-service and wallet-service listen to this and update their local records.
 */
public record PlanUpgradedEvent(
        UUID userId,
        String email,
        String previousPlan,
        String newPlan,
        LocalDateTime timestamp
) {
    public static PlanUpgradedEvent create(UUID userId, String email,
                                            String previousPlan, String newPlan) {
        return new PlanUpgradedEvent(userId, email, previousPlan, newPlan, LocalDateTime.now());
    }
}
