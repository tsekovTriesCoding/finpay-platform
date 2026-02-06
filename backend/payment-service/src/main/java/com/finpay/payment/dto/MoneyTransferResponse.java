package com.finpay.payment.dto;

import com.finpay.payment.entity.MoneyTransfer;

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
        MoneyTransfer.TransferStatus status,
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
                transfer.getStatus(),
                transfer.getFailureReason(),
                transfer.getCompletedAt(),
                transfer.getCreatedAt()
        );
    }
}
