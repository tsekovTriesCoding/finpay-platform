package com.finpay.payment.billpayment.dto;

import com.finpay.payment.billpayment.BillPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BillPaymentResponse(
        UUID id,
        UUID userId,
        String transactionReference,
        BillPayment.BillCategory category,
        String billerName,
        String billerCode,
        String accountNumber,
        String accountHolderName,
        BigDecimal amount,
        String currency,
        BigDecimal processingFee,
        BigDecimal totalAmount,
        BillPayment.BillPaymentStatus status,
        String description,
        String failureReason,
        String billerReference,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static BillPaymentResponse fromEntity(BillPayment bp) {
        return new BillPaymentResponse(
                bp.getId(),
                bp.getUserId(),
                bp.getTransactionReference(),
                bp.getCategory(),
                bp.getBillerName(),
                bp.getBillerCode(),
                bp.getAccountNumber(),
                bp.getAccountHolderName(),
                bp.getAmount(),
                bp.getCurrency(),
                bp.getProcessingFee(),
                bp.getTotalAmount(),
                bp.getStatus(),
                bp.getDescription(),
                bp.getFailureReason(),
                bp.getBillerReference(),
                bp.getProcessedAt(),
                bp.getCreatedAt(),
                bp.getUpdatedAt()
        );
    }
}
