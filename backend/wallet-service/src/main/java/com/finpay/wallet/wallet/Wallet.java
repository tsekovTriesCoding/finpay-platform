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

    @Column(nullable = false) @Enumerated(EnumType.STRING)
    private WalletStatus status;

    @Version
    private Long version;

    @CreationTimestamp @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum WalletStatus { ACTIVE, FROZEN, CLOSED }

    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    public boolean reserveFunds(BigDecimal amount) {
        if (getAvailableBalance().compareTo(amount) >= 0) {
            this.reservedBalance = this.reservedBalance.add(amount);
            return true;
        }
        return false;
    }

    public void releaseReservedFunds(BigDecimal amount) {
        this.reservedBalance = this.reservedBalance.subtract(amount);
        if (this.reservedBalance.compareTo(BigDecimal.ZERO) < 0) {
            this.reservedBalance = BigDecimal.ZERO;
        }
    }

    public void deductReservedFunds(BigDecimal amount) {
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.balance = this.balance.subtract(amount);
    }

    public void creditFunds(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
