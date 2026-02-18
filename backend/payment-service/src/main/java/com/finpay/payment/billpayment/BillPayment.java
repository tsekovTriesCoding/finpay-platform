package com.finpay.payment.billpayment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a bill payment (utilities, internet, phone, etc.).
 * Follows the event-driven pattern: payment is initiated, then processed
 * via Kafka events through the wallet-service for fund deduction.
 */
@Entity
@Table(name = "bill_payments", indexes = {
        @Index(name = "idx_bill_user", columnList = "userId"),
        @Index(name = "idx_bill_status", columnList = "status"),
        @Index(name = "idx_bill_reference", columnList = "transactionReference", unique = true),
        @Index(name = "idx_bill_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String transactionReference;

    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    private BillCategory category;

    @Column(nullable = false)
    private String billerName;

    @Column(nullable = false)
    private String billerCode;

    @Column(nullable = false)
    private String accountNumber;

    private String accountHolderName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(precision = 19, scale = 4)
    private BigDecimal processingFee;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BillPaymentStatus status = BillPaymentStatus.PENDING;

    private String description;

    private String failureReason;

    // Wallet integration - saga tracking
    private UUID walletId;

    @Builder.Default
    private boolean fundsReserved = false;

    @Builder.Default
    private boolean fundsDeducted = false;

    @Builder.Default
    private boolean billerConfirmed = false;

    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SagaStatus sagaStatus = SagaStatus.INITIATED;

    @Builder.Default
    private boolean compensationRequired = false;

    @Builder.Default
    private boolean compensationCompleted = false;

    // External biller response
    private String billerReference;
    private String billerResponse;

    private LocalDateTime processedAt;
    private LocalDateTime failedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Enums

    public enum BillCategory {
        ELECTRICITY,
        WATER,
        INTERNET,
        PHONE,
        GAS,
        INSURANCE,
        RENT,
        SUBSCRIPTION,
        GOVERNMENT,
        EDUCATION,
        OTHER
    }

    public enum BillPaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED,
        COMPENSATING,
        COMPENSATED
    }

    public enum SagaStatus {
        INITIATED,
        FUNDS_RESERVED,
        FUNDS_DEDUCTED,
        BILLER_CONFIRMED,
        COMPLETED,
        FAILED,
        COMPENSATING,
        COMPENSATED
    }
}
