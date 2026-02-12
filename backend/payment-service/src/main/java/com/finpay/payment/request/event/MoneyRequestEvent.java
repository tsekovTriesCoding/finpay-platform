package com.finpay.payment.request.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published to Kafka for money request lifecycle notifications.
 * Consumed by notification-service to alert users about incoming requests,
 * approvals, declines, cancellations, and completed payments.
 */
public record MoneyRequestEvent(
        UUID requestId,
        String requestReference,
        UUID requesterUserId,
        UUID payerUserId,
        BigDecimal amount,
        String currency,
        String description,
        EventType eventType,
        String failureReason,
        LocalDateTime timestamp
) {
    public enum EventType {
        REQUEST_CREATED,      // Notify payer about incoming request
        REQUEST_APPROVED,     // Notify requester that payer approved
        REQUEST_DECLINED,     // Notify requester that payer declined
        REQUEST_CANCELLED,    // Notify payer that requester cancelled
        REQUEST_EXPIRED,      // Notify both parties
        REQUEST_COMPLETED,    // Notify both parties (funds transferred)
        REQUEST_FAILED        // Notify both parties (saga failed)
    }

    public static MoneyRequestEvent create(UUID requestId, String requestReference,
                                            UUID requesterUserId, UUID payerUserId,
                                            BigDecimal amount, String currency,
                                            String description, EventType eventType) {
        return new MoneyRequestEvent(
                requestId, requestReference, requesterUserId, payerUserId,
                amount, currency, description, eventType, null, LocalDateTime.now()
        );
    }

    public static MoneyRequestEvent withFailure(UUID requestId, String requestReference,
                                                 UUID requesterUserId, UUID payerUserId,
                                                 BigDecimal amount, String currency,
                                                 String description, EventType eventType,
                                                 String failureReason) {
        return new MoneyRequestEvent(
                requestId, requestReference, requesterUserId, payerUserId,
                amount, currency, description, eventType, failureReason, LocalDateTime.now()
        );
    }
}
