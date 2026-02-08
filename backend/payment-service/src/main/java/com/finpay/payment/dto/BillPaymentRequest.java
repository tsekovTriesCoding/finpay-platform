package com.finpay.payment.dto;

import com.finpay.payment.entity.BillPayment;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record BillPaymentRequest(

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Bill category is required")
        BillPayment.BillCategory category,

        @NotBlank(message = "Biller name is required")
        String billerName,

        @NotBlank(message = "Biller code is required")
        String billerCode,

        @NotBlank(message = "Account number is required")
        String accountNumber,

        String accountHolderName,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        String description
) {
    /**
     * Return a normalised currency, defaulting to USD.
     */
    public String effectiveCurrency() {
        return currency != null && !currency.isBlank() ? currency.toUpperCase() : "USD";
    }
}
