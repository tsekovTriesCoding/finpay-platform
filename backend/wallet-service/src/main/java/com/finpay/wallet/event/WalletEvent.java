package com.finpay.wallet.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event for wallet operations.
 * Used to communicate wallet state changes via Kafka.
 */
public record WalletEvent(
        UUID eventId,
        UUID walletId,
        UUID userId,
        EventType eventType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String currency,
        String referenceId,
        String description,
        LocalDateTime timestamp
) {
    public enum EventType {
        WALLET_CREATED,
        FUNDS_RESERVED,
        FUNDS_RELEASED,
        FUNDS_DEBITED,
        FUNDS_CREDITED,
        DEPOSIT,
        WITHDRAWAL,
        WALLET_FROZEN,
        WALLET_UNFROZEN
    }

    public static WalletEvent create(UUID walletId, UUID userId, EventType eventType,
                                      BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                      String currency, String referenceId, String description) {
        return new WalletEvent(
                UUID.randomUUID(),
                walletId,
                userId,
                eventType,
                amount,
                balanceBefore,
                balanceAfter,
                currency,
                referenceId,
                description,
                LocalDateTime.now()
        );
    }
}
