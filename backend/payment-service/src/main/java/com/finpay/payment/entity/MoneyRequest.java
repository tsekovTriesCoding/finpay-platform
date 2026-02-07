package com.finpay.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Money Request entity tracking P2P payment requests between users.
 * Uses SAGA pattern for distributed transaction management when approved.
 *
 * Flow:
 * 1. Requester creates request → PENDING_APPROVAL
 * 2. Payer is notified via Kafka
 * 3a. Payer approves → SAGA starts (same as transfer: RESERVE → DEDUCT → CREDIT → NOTIFY)
 * 3b. Payer declines → DECLINED
 * 4. Requester can cancel → CANCELLED
 * 5. Expiration → EXPIRED (handled by scheduled job)
 */
@Entity
@Table(name = "money_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String requestReference;

    /** The user who is requesting money (will receive funds on approval). */
    @Column(nullable = false)
    private UUID requesterUserId;

    /** The user who is being asked to pay (will send funds on approval). */
    @Column(nullable = false)
    private UUID payerUserId;

    // Wallet IDs populated by wallet-service via Kafka events during SAGA
    private UUID requesterWalletId;
    private UUID payerWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    /** SAGA status - only relevant once the request is approved and payment begins. */
    @Enumerated(EnumType.STRING)
    private SagaStatus sagaStatus;

    private String failureReason;

    // SAGA step tracking (only used after approval)
    private boolean fundsReserved;
    private boolean fundsDeducted;
    private boolean fundsCredited;
    private boolean notificationSent;

    // Compensation tracking
    private boolean compensationRequired;
    private boolean compensationCompleted;

    private LocalDateTime approvedAt;
    private LocalDateTime declinedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum RequestStatus {
        PENDING_APPROVAL, // Waiting for payer to approve or decline
        APPROVED,         // Payer approved, SAGA starting
        PROCESSING,       // SAGA in progress (funds being moved)
        COMPLETED,        // Successfully completed
        DECLINED,         // Payer declined
        CANCELLED,        // Requester cancelled
        FAILED,           // SAGA failed during processing
        EXPIRED,          // Request expired before action was taken
        COMPENSATING,     // Rollback in progress
        COMPENSATED       // Rollback completed
    }

    public enum SagaStatus {
        NOT_STARTED,
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
