package com.finpay.user.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log entity tracking WHO did WHAT to WHICH resource WHEN.
 * Critical for fintech compliance (PCI-DSS, SOX).
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actorId"),
        @Index(name = "idx_audit_target", columnList = "targetType, targetId"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created", columnList = "createdAt"),
        @Index(name = "idx_audit_actor_action", columnList = "actorId, action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private UUID id;

    @PrePersist
    void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    /** The user (admin) who performed the action */
    @Column(nullable = false)
    private UUID actorId;

    /** Email of the actor for display purposes */
    @Column(nullable = false)
    private String actorEmail;

    /** The action performed */
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private AuditAction action;

    /** The type of resource affected */
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    /** The ID of the resource affected */
    @Column(nullable = false)
    private String targetId;

    /** Human-readable description of the change */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Previous state (JSON) — for state-change auditing */
    @Column(columnDefinition = "TEXT")
    private String previousState;

    /** New state (JSON) — for state-change auditing */
    @Column(columnDefinition = "TEXT")
    private String newState;

    /** IP address of the actor */
    private String ipAddress;

    /** The service where the action originated */
    @Column(nullable = false)
    private String serviceSource;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AuditAction {
        // User management
        USER_ROLE_CHANGED,
        USER_SUSPENDED,
        USER_UNSUSPENDED,
        USER_DELETED,
        USER_FORCE_PASSWORD_RESET,
        USER_IMPERSONATED,

        // Wallet management
        WALLET_FROZEN,
        WALLET_UNFROZEN,
        WALLET_CLOSED,

        // Transaction management
        TRANSACTION_FLAGGED,
        TRANSACTION_UNFLAGGED,
        TRANSACTION_DISPUTE_OPENED,
        TRANSACTION_DISPUTE_RESOLVED,

        // Notification
        NOTIFICATION_BROADCAST_SENT,

        // System
        SYSTEM_CONFIG_CHANGED,
        BULK_ACTION_EXECUTED
    }

    public enum TargetType {
        USER,
        WALLET,
        TRANSFER,
        BILL_PAYMENT,
        MONEY_REQUEST,
        PAYMENT,
        NOTIFICATION,
        SYSTEM
    }
}
