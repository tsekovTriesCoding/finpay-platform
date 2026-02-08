package com.finpay.payment.event;

import com.finpay.payment.entity.BillPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka event emitted for every bill payment state change.
 * Consumed by notification-service and any analytics/audit pipeline.
 */
public record BillPaymentEvent(
        UUID billPaymentId,
        UUID userId,
        String transactionReference,
        BillPayment.BillCategory category,
        String billerName,
        String billerCode,
        String accountNumber,
        BigDecimal amount,
        String currency,
        BillPayment.BillPaymentStatus status,
        EventType eventType,
        String failureReason,
        LocalDateTime timestamp
) {

    public enum EventType {
        BILL_PAYMENT_INITIATED,
        BILL_PAYMENT_PROCESSING,
        BILL_PAYMENT_COMPLETED,
        BILL_PAYMENT_FAILED,
        BILL_PAYMENT_CANCELLED,
        BILL_PAYMENT_REFUNDED
    }

    public static BillPaymentEvent of(BillPayment bp, EventType eventType) {
        return new BillPaymentEvent(
                bp.getId(),
                bp.getUserId(),
                bp.getTransactionReference(),
                bp.getCategory(),
                bp.getBillerName(),
                bp.getBillerCode(),
                bp.getAccountNumber(),
                bp.getAmount(),
                bp.getCurrency(),
                bp.getStatus(),
                eventType,
                bp.getFailureReason(),
                LocalDateTime.now()
        );
    }
}
