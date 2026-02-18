package com.finpay.wallet.transaction;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "VARCHAR(20)") @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 3)
    private String currency;

    private String referenceId;
    private String description;

    @Column(nullable = false, columnDefinition = "VARCHAR(20)") @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @CreationTimestamp @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN,
        RESERVE, RELEASE_RESERVE, DEBIT, CREDIT
    }

    public enum TransactionStatus { PENDING, COMPLETED, FAILED, REVERSED }
}
