package com.finpay.wallet.saga.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletResponseEvent(
        UUID eventId, UUID correlationId, UUID walletId, UUID userId,
        ResponseType responseType, BigDecimal amount, BigDecimal newBalance,
        BigDecimal newAvailableBalance, String currency, boolean success,
        String failureReason, LocalDateTime timestamp
) {
    public enum ResponseType {
        FUNDS_RESERVED, FUNDS_RELEASED, FUNDS_DEDUCTED, FUNDS_CREDITED,
        CREDIT_REVERSED, DEDUCTION_REVERSED, OPERATION_FAILED
    }

    public static WalletResponseEvent success(UUID correlationId, UUID walletId, UUID userId,
                                               ResponseType type, BigDecimal amount,
                                               BigDecimal newBalance, BigDecimal newAvailableBalance,
                                               String currency) {
        return new WalletResponseEvent(UUID.randomUUID(), correlationId, walletId, userId,
                type, amount, newBalance, newAvailableBalance, currency, true, null, LocalDateTime.now());
    }

    public static WalletResponseEvent failure(UUID correlationId, UUID userId, ResponseType type,
                                               BigDecimal amount, String currency, String reason) {
        return new WalletResponseEvent(UUID.randomUUID(), correlationId, null, userId,
                type, amount, null, null, currency, false, reason, LocalDateTime.now());
    }
}
