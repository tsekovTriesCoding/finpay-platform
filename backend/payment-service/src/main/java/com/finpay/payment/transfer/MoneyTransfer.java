package com.finpay.payment.transfer;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Money Transfer entity tracking P2P transfers between users.
 * Uses SAGA pattern for distributed transaction management.
 */
@Entity
@Table(name = "money_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String transactionReference;

    @Column(nullable = false)
    private UUID senderUserId;

    // Wallet IDs are populated by wallet-service via Kafka events
    private UUID senderWalletId;

    @Column(nullable = false)
    private UUID recipientUserId;

    // Wallet IDs are populated by wallet-service via Kafka events
    private UUID recipientWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String description;

    /** Distinguishes direct sends from request-triggered payments. */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransferType transferType = TransferType.SEND;

    /** When this transfer was created from a money request, stores the request ID. */
    private UUID sourceRequestId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SagaStatus sagaStatus;

    private String failureReason;

    // SAGA step tracking
    private boolean fundsReserved;
    private boolean fundsDeducted;
    private boolean fundsCredit;
    private boolean notificationSent;

    // Compensation tracking
    private boolean compensationRequired;
    private boolean compensationCompleted;

    private LocalDateTime completedAt;
    private LocalDateTime failedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum TransferType {
        SEND,              // Direct P2P send
        REQUEST_PAYMENT    // Payment triggered by an approved money request
    }

    public enum TransferStatus {
        PENDING,          // Initial state
        PROCESSING,       // SAGA in progress
        COMPLETED,        // Successfully completed
        FAILED,           // Failed during SAGA
        CANCELLED,        // Cancelled by user
        COMPENSATING,     // Rollback in progress
        COMPENSATED       // Rollback completed
    }

    public enum SagaStatus {
        STARTED,
        FUNDS_RESERVED,
        FUNDS_DEDUCTED,
        FUNDS_CREDITED,
        NOTIFICATION_SENT,
        COMPLETED,
        FAILED,
        COMPENSATING,
        COMPENSATED
    }
}
