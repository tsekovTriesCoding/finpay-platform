package com.finpay.payment.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String transactionReference;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    private String description;

    // Source account details
    private String sourceAccountNumber;
    private String sourceAccountName;
    private String sourceBankCode;

    // Destination account details
    private String destinationAccountNumber;
    private String destinationAccountName;
    private String destinationBankCode;

    // Card details (masked)
    private String cardLastFourDigits;
    private String cardType;

    // External payment gateway reference
    private String gatewayReference;
    private String gatewayResponse;

    // Fees
    @Column(precision = 19, scale = 4)
    private BigDecimal processingFee;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount;

    private String failureReason;

    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED
    }

    public enum PaymentMethod {
        CARD,
        BANK_TRANSFER,
        WALLET,
        MOBILE_MONEY
    }

    public enum PaymentType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER,
        PAYMENT
    }
}
