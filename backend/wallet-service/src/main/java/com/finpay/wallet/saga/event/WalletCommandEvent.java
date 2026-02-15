package com.finpay.wallet.saga.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletCommandEvent(
        UUID eventId, UUID correlationId, UUID userId, CommandType command,
        BigDecimal amount, String currency, String description, LocalDateTime timestamp
) {
    public enum CommandType {
        RESERVE_FUNDS, RELEASE_FUNDS, DEDUCT_FUNDS,
        CREDIT_FUNDS, REVERSE_CREDIT, REVERSE_DEDUCTION
    }

    public static WalletCommandEvent create(UUID correlationId, UUID userId, CommandType command,
                                             BigDecimal amount, String currency, String description) {
        return new WalletCommandEvent(UUID.randomUUID(), correlationId, userId, command,
                amount, currency, description, LocalDateTime.now());
    }
}
