package com.finpay.payment.event;

import com.finpay.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentEvent(
        UUID paymentId,
        UUID userId,
        String transactionReference,
        BigDecimal amount,
        String currency,
        Payment.PaymentStatus status,
        Payment.PaymentMethod paymentMethod,
        Payment.PaymentType paymentType,
        EventType eventType,
        String failureReason,
        LocalDateTime timestamp
) {
    public enum EventType {
        PAYMENT_INITIATED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        PAYMENT_CANCELLED,
        PAYMENT_REFUNDED
    }
}
