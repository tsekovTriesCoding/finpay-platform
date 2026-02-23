package com.finpay.wallet.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value object that tracks daily and monthly spend totals with
 * automatic calendar-based resets.  Embedded inside {@link Wallet}
 * so the columns live in the same {@code wallets} table.
 */
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SpendTracker {

    @Column(nullable = false, precision = 19, scale = 4,
            columnDefinition = "DECIMAL(19,4) NOT NULL DEFAULT 0")
    @Builder.Default
    private BigDecimal dailySpent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4,
            columnDefinition = "DECIMAL(19,4) NOT NULL DEFAULT 0")
    @Builder.Default
    private BigDecimal monthlySpent = BigDecimal.ZERO;

    @Column(nullable = false, columnDefinition = "DATE NOT NULL DEFAULT (CURRENT_DATE)")
    @Builder.Default
    private LocalDate lastDailyReset = LocalDate.now();

    @Column(nullable = false, columnDefinition = "DATE NOT NULL DEFAULT (CURRENT_DATE)")
    @Builder.Default
    private LocalDate lastMonthlyReset = LocalDate.now();

    // Auto-reset logic

    /**
     * Resets daily/monthly counters when the calendar day or month has
     * rolled over.  Safe to call multiple times per request.
     */
    public void resetIfNeeded() {
        LocalDate today = LocalDate.now();

        if (!today.equals(lastDailyReset)) {
            this.dailySpent = BigDecimal.ZERO;
            this.lastDailyReset = today;
        }
        if (today.getMonth() != lastMonthlyReset.getMonth()
                || today.getYear() != lastMonthlyReset.getYear()) {
            this.monthlySpent = BigDecimal.ZERO;
            this.lastMonthlyReset = today;
        }
    }

    // Remaining allowance queries

    public BigDecimal remainingDaily(BigDecimal dailyLimit) {
        resetIfNeeded();
        return dailyLimit.subtract(dailySpent).max(BigDecimal.ZERO);
    }

    public BigDecimal remainingMonthly(BigDecimal monthlyLimit) {
        resetIfNeeded();
        return monthlyLimit.subtract(monthlySpent).max(BigDecimal.ZERO);
    }

    // Recording / rolling-back spend

    public void recordSpend(BigDecimal amount) {
        this.dailySpent = this.dailySpent.add(amount);
        this.monthlySpent = this.monthlySpent.add(amount);
    }

    public void rollbackSpend(BigDecimal amount) {
        this.dailySpent = this.dailySpent.subtract(amount).max(BigDecimal.ZERO);
        this.monthlySpent = this.monthlySpent.subtract(amount).max(BigDecimal.ZERO);
    }
}
