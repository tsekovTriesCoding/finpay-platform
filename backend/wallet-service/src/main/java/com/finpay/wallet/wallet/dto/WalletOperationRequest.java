package com.finpay.wallet.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record WalletOperationRequest(
        @NotNull(message = "User ID is required") UUID userId,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount,
        String referenceId,
        String description
) {}
