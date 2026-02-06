package com.finpay.wallet.dto;

import com.finpay.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        BigDecimal reservedBalance,
        BigDecimal availableBalance,
        String currency,
        Wallet.WalletStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WalletResponse fromEntity(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getReservedBalance(),
                wallet.getAvailableBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
