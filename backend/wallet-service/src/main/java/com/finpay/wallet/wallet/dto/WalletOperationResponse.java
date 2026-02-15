package com.finpay.wallet.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletOperationResponse(
        UUID walletId, UUID userId, String operation, BigDecimal amount,
        BigDecimal newBalance, BigDecimal newAvailableBalance,
        boolean success, String message, LocalDateTime timestamp
) {
    public static WalletOperationResponse success(UUID walletId, UUID userId, String operation,
                                                   BigDecimal amount, BigDecimal newBalance,
                                                   BigDecimal newAvailableBalance) {
        return new WalletOperationResponse(walletId, userId, operation, amount, newBalance,
                newAvailableBalance, true, "Operation completed successfully", LocalDateTime.now());
    }

    public static WalletOperationResponse failure(UUID userId, String operation,
                                                   BigDecimal amount, String message) {
        return new WalletOperationResponse(null, userId, operation, amount, null, null,
                false, message, LocalDateTime.now());
    }
}
