package com.finpay.auth.dto;

public record UpgradePlanResponse(
        String userId,
        String previousPlan,
        String newPlan,
        String message
) {
    public static UpgradePlanResponse success(String userId, String previousPlan, String newPlan) {
        return new UpgradePlanResponse(userId, previousPlan, newPlan,
                "Plan upgraded successfully from " + previousPlan + " to " + newPlan);
    }
}
