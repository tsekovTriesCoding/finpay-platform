package com.finpay.auth.dto;

import com.finpay.auth.entity.AccountPlan;
import jakarta.validation.constraints.NotNull;

public record UpgradePlanRequest(
        @NotNull(message = "New plan is required")
        AccountPlan newPlan
) {}
