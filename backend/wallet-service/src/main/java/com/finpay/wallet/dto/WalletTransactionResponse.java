package com.finpay.wallet.dto;

import com.finpay.wallet.entity.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        UUID walletId,
        UUID userId,
        WalletTransaction.TransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String currency,
        String referenceId,
        String description,
        WalletTransaction.TransactionStatus status,
        LocalDateTime createdAt
) {
    public static WalletTransactionResponse fromEntity(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getWalletId(),
                transaction.getUserId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getCurrency(),
                transaction.getReferenceId(),
                transaction.getDescription(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
