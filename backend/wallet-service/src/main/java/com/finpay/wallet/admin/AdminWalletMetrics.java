package com.finpay.wallet.admin;

import java.math.BigDecimal;

public record AdminWalletMetrics(
        long totalWallets,
        long activeWallets,
        long frozenWallets,
        long closedWallets,
        BigDecimal totalBalance
) {
}
