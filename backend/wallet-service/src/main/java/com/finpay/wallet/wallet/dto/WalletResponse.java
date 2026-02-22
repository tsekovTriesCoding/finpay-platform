package com.finpay.wallet.wallet.dto;

import com.finpay.wallet.wallet.Wallet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletResponse(
        UUID id, UUID userId, BigDecimal balance, BigDecimal reservedBalance,
        BigDecimal availableBalance, String currency, Wallet.WalletStatus status,
        Wallet.AccountPlan plan, BigDecimal dailyTransactionLimit,
        BigDecimal monthlyTransactionLimit, Integer maxVirtualCards,
        Boolean multiCurrencyEnabled, Boolean apiAccessEnabled,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {}
