package com.finpay.payment.transfer.dto;

import com.finpay.payment.transfer.MoneyTransfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MoneyTransferResponse(
        UUID id,
        String transactionReference,
        UUID senderUserId,
        UUID recipientUserId,
        BigDecimal amount,
        String currency,
        String description,
        MoneyTransfer.TransferType transferType,
        MoneyTransfer.TransferStatus status,
        UUID sourceRequestId,
        String failureReason,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public static MoneyTransferResponse fromEntity(MoneyTransfer transfer) {
        return new MoneyTransferResponse(
                transfer.getId(),
                transfer.getTransactionReference(),
                transfer.getSenderUserId(),
                transfer.getRecipientUserId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getDescription(),
                transfer.getTransferType(),
                transfer.getStatus(),
                transfer.getSourceRequestId(),
                transfer.getFailureReason(),
                transfer.getCompletedAt(),
                transfer.getCreatedAt()
        );
    }
}
