package com.finpay.payment.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unified admin transaction view combining transfers, bill payments, money requests.
 */
public record AdminTransactionResponse(
        UUID id,
        String type,            // TRANSFER, BILL_PAYMENT, MONEY_REQUEST
        String transactionReference,
        UUID fromUserId,
        UUID toUserId,
        BigDecimal amount,
        String currency,
        String status,
        String description,
        boolean flagged,
        String flagReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
