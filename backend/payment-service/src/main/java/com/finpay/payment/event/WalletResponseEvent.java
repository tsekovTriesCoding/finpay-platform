package com.finpay.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response events received from wallet-service after processing commands.
 */
public record WalletResponseEvent(
        UUID eventId,
        UUID correlationId,      // Transfer ID - links all events in a saga
        UUID walletId,
        UUID userId,
        ResponseType responseType,
        BigDecimal amount,
        BigDecimal newBalance,
        BigDecimal newAvailableBalance,
        String currency,
        boolean success,
        String failureReason,
        LocalDateTime timestamp
) {
    public enum ResponseType {
        FUNDS_RESERVED,
        FUNDS_RELEASED,
        FUNDS_DEDUCTED,
        FUNDS_CREDITED,
        CREDIT_REVERSED,
        DEDUCTION_REVERSED,
        OPERATION_FAILED
    }
}
