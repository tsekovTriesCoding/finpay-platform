package com.finpay.payment.detail.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unified transaction detail response used across all transaction types
 * (transfer, bill payment, money request). Provides a consistent shape
 * for the frontend detail sheet including receipt data, status timeline,
 * and available actions (dispute, cancel, etc.).
 */
public record TransactionDetailResponse(
        UUID id,
        String transactionReference,
        TransactionType type,

        // Parties
        UUID senderUserId,
        UUID recipientUserId,

        // Amounts
        BigDecimal amount,
        String currency,
        BigDecimal processingFee,
        BigDecimal totalAmount,

        // Status
        String status,
        String failureReason,

        // Descriptive
        String title,
        String subtitle,
        String description,

        // Metadata (type-specific fields)
        Map<String, Object> metadata,

        // Status timeline
        List<StatusTimelineEntry> timeline,

        // Available actions
        List<String> availableActions,

        // Timestamps
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {

    public enum TransactionType {
        TRANSFER,
        BILL_PAYMENT,
        MONEY_REQUEST
    }
}
