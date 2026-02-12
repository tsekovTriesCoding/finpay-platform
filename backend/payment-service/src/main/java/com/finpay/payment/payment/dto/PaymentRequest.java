package com.finpay.payment.payment.dto;

import com.finpay.payment.payment.Payment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotNull(message = "Payment method is required")
        Payment.PaymentMethod paymentMethod,

        @NotNull(message = "Payment type is required")
        Payment.PaymentType paymentType,

        String description,
        String destinationAccountNumber,
        String destinationAccountName,
        String destinationBankCode,
        UUID paymentMethodId,
        String cardNumber,
        String cardExpiryMonth,
        String cardExpiryYear,
        String cardCvv,
        String cardHolderName
) {}
