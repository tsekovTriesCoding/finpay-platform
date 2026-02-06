package com.finpay.wallet.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command events that wallet-service listens for.
 * Published by payment-service to request wallet operations.
 */
public record WalletCommandEvent(
        UUID eventId,
        UUID correlationId,      // Transfer ID - links all events in a saga
        UUID userId,
        CommandType command,
        BigDecimal amount,
        String currency,
        String description,
        LocalDateTime timestamp
) {
    public enum CommandType {
        RESERVE_FUNDS,
        RELEASE_FUNDS,
        DEDUCT_FUNDS,
        CREDIT_FUNDS,
        REVERSE_CREDIT,
        REVERSE_DEDUCTION
    }

    public static WalletCommandEvent create(UUID correlationId, UUID userId, CommandType command,
                                             BigDecimal amount, String currency, String description) {
        return new WalletCommandEvent(
                UUID.randomUUID(),
                correlationId,
                userId,
                command,
                amount,
                currency,
                description,
                LocalDateTime.now()
        );
    }
}
