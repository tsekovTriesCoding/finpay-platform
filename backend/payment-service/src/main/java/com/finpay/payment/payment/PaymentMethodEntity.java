package com.finpay.payment.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MethodType type;

    // For cards
    private String cardHolderName;
    private String cardLastFourDigits;
    private String cardBrand; // VISA, MASTERCARD, etc.
    private String cardExpiryMonth;
    private String cardExpiryYear;
    private String cardToken; // Tokenized card from payment gateway

    // For bank accounts
    private String bankName;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String accountType;

    // For mobile money
    private String mobileProvider;
    private String mobileNumber;

    @Column(nullable = false)
    private boolean isDefault;

    @Column(nullable = false)
    private boolean isVerified;

    @Column(nullable = false)
    private boolean isActive;

    private String nickname;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum MethodType {
        CARD,
        BANK_ACCOUNT,
        MOBILE_MONEY,
        WALLET
    }
}
