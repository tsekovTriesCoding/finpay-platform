package com.finpay.wallet.transaction.dto;

import com.finpay.wallet.transaction.WalletTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id, UUID walletId, UUID userId, WalletTransaction.TransactionType type,
        BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
        String currency, String referenceId, String description,
        WalletTransaction.TransactionStatus status, LocalDateTime createdAt
) {}
