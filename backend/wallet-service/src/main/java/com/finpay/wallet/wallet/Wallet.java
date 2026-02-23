package com.finpay.wallet.wallet;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedBalance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, columnDefinition = "VARCHAR(20)") @Enumerated(EnumType.STRING)
    private WalletStatus status;

    @Column(nullable = false, columnDefinition = "VARCHAR(20)") @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountPlan plan = AccountPlan.STARTER;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyTransactionLimit = new BigDecimal("500.00");

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyTransactionLimit = new BigDecimal("5000.00");

    // Stracking (delegated to embeddable)

    @Embedded
    @Builder.Default
    private SpendTracker spendTracker = new SpendTracker();

    // Plan feature flags

    @Column(nullable = false)
    @Builder.Default
    private Integer maxVirtualCards = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean multiCurrencyEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean apiAccessEnabled = false;

    @Version
    private Long version;

    @CreationTimestamp @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum WalletStatus { ACTIVE, FROZEN, CLOSED }

    /**
     * Subscription plans that determine wallet capabilities and limits.
     */
    public enum AccountPlan {
        STARTER, PRO, ENTERPRISE
    }

    // Balance helpers

    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    // Spend-limit delegates

    public BigDecimal getRemainingDailyLimit() {
        return spendTracker.remainingDaily(dailyTransactionLimit);
    }

    public BigDecimal getRemainingMonthlyLimit() {
        return spendTracker.remainingMonthly(monthlyTransactionLimit);
    }

    // Funds operations

    public boolean reserveFunds(BigDecimal amount) {
        if (getAvailableBalance().compareTo(amount) >= 0) {
            this.reservedBalance = this.reservedBalance.add(amount);
            spendTracker.recordSpend(amount);
            return true;
        }
        return false;
    }

    public void releaseReservedFunds(BigDecimal amount) {
        this.reservedBalance = this.reservedBalance.subtract(amount);
        if (this.reservedBalance.compareTo(BigDecimal.ZERO) < 0) {
            this.reservedBalance = BigDecimal.ZERO;
        }
        spendTracker.rollbackSpend(amount);
    }

    public void deductReservedFunds(BigDecimal amount) {
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.balance = this.balance.subtract(amount);
    }

    public void creditFunds(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
