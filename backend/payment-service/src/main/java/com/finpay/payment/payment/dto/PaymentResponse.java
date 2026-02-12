package com.finpay.payment.payment.dto;

import com.finpay.payment.payment.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID userId,
        String transactionReference,
        BigDecimal amount,
        String currency,
        Payment.PaymentStatus status,
        Payment.PaymentMethod paymentMethod,
        Payment.PaymentType paymentType,
        String description,
        String destinationAccountNumber,
        String destinationAccountName,
        String destinationBankCode,
        String cardLastFourDigits,
        String cardType,
        BigDecimal processingFee,
        BigDecimal totalAmount,
        String failureReason,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse fromEntity(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getTransactionReference(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getPaymentType(),
                payment.getDescription(),
                payment.getDestinationAccountNumber(),
                payment.getDestinationAccountName(),
                payment.getDestinationBankCode(),
                payment.getCardLastFourDigits(),
                payment.getCardType(),
                payment.getProcessingFee(),
                payment.getTotalAmount(),
                payment.getFailureReason(),
                payment.getProcessedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
