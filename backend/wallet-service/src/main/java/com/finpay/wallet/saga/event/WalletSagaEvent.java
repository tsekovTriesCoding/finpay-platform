package com.finpay.wallet.saga.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletSagaEvent(
        UUID sagaId, UUID correlationId, UUID walletId, UUID userId,
        SagaStep step, SagaAction action, BigDecimal amount, String currency,
        String referenceId, boolean success, String failureReason, LocalDateTime timestamp
) {
    public enum SagaStep {
        RESERVE_FUNDS, RELEASE_FUNDS, DEDUCT_FUNDS,
        CREDIT_FUNDS, REVERSE_CREDIT, REVERSE_DEDUCTION
    }

    public enum SagaAction { REQUEST, CONFIRM, COMPENSATE, FAIL }

    public static WalletSagaEvent request(UUID correlationId, UUID userId, SagaStep step,
                                           BigDecimal amount, String currency, String referenceId) {
        return new WalletSagaEvent(UUID.randomUUID(), correlationId, null, userId, step,
                SagaAction.REQUEST, amount, currency, referenceId, false, null, LocalDateTime.now());
    }

    public WalletSagaEvent confirm(UUID walletId) {
        return new WalletSagaEvent(sagaId, correlationId, walletId, userId, step,
                SagaAction.CONFIRM, amount, currency, referenceId, true, null, LocalDateTime.now());
    }

    public WalletSagaEvent fail(String reason) {
        return new WalletSagaEvent(sagaId, correlationId, walletId, userId, step,
                SagaAction.FAIL, amount, currency, referenceId, false, reason, LocalDateTime.now());
    }
}
