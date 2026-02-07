package com.finpay.payment.dto;

import com.finpay.payment.entity.MoneyRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for money request operations.
 */
public record MoneyRequestResponse(
        UUID id,
        String requestReference,
        UUID requesterUserId,
        UUID payerUserId,
        BigDecimal amount,
        String currency,
        String description,
        MoneyRequest.RequestStatus status,
        MoneyRequest.SagaStatus sagaStatus,
        String failureReason,
        LocalDateTime approvedAt,
        LocalDateTime declinedAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static MoneyRequestResponse fromEntity(MoneyRequest request) {
        return new MoneyRequestResponse(
                request.getId(),
                request.getRequestReference(),
                request.getRequesterUserId(),
                request.getPayerUserId(),
                request.getAmount(),
                request.getCurrency(),
                request.getDescription(),
                request.getStatus(),
                request.getSagaStatus(),
                request.getFailureReason(),
                request.getApprovedAt(),
                request.getDeclinedAt(),
                request.getCompletedAt(),
                request.getExpiresAt(),
                request.getCreatedAt()
        );
    }
}
