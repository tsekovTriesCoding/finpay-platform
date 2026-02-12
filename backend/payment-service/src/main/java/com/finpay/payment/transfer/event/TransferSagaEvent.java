package com.finpay.payment.transfer.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SAGA event for money transfer operations.
 * Used to coordinate distributed transactions via Kafka.
 */
public record TransferSagaEvent(
        UUID transferId,
        String transactionReference,
        UUID senderUserId,
        UUID senderWalletId,
        UUID recipientUserId,
        UUID recipientWalletId,
        BigDecimal amount,
        String currency,
        String description,
        SagaStep sagaStep,
        SagaAction action,
        String failureReason,
        LocalDateTime timestamp
) {
    public enum SagaStep {
        INITIATE,           // Start the SAGA
        RESERVE_FUNDS,      // Reserve funds from sender
        DEDUCT_FUNDS,       // Deduct from sender wallet
        CREDIT_FUNDS,       // Credit to recipient wallet
        SEND_NOTIFICATION,  // Notify users
        COMPLETE,           // SAGA completed successfully
        COMPENSATE          // Rollback required
    }

    public enum SagaAction {
        EXECUTE,            // Forward action
        COMPENSATE,         // Rollback action
        CONFIRM,            // Step completed successfully
        FAIL                // Step failed
    }

    public static TransferSagaEvent initiate(UUID transferId, String transactionReference,
                                              UUID senderUserId, UUID senderWalletId,
                                              UUID recipientUserId, UUID recipientWalletId,
                                              BigDecimal amount, String currency, String description) {
        return new TransferSagaEvent(
                transferId, transactionReference, senderUserId, senderWalletId,
                recipientUserId, recipientWalletId, amount, currency, description,
                SagaStep.INITIATE, SagaAction.EXECUTE, null, LocalDateTime.now()
        );
    }

    public TransferSagaEvent withStep(SagaStep step, SagaAction action) {
        return new TransferSagaEvent(
                transferId, transactionReference, senderUserId, senderWalletId,
                recipientUserId, recipientWalletId, amount, currency, description,
                step, action, null, LocalDateTime.now()
        );
    }

    public TransferSagaEvent withFailure(SagaStep step, String reason) {
        return new TransferSagaEvent(
                transferId, transactionReference, senderUserId, senderWalletId,
                recipientUserId, recipientWalletId, amount, currency, description,
                step, SagaAction.FAIL, reason, LocalDateTime.now()
        );
    }

    public TransferSagaEvent compensate(String reason) {
        return new TransferSagaEvent(
                transferId, transactionReference, senderUserId, senderWalletId,
                recipientUserId, recipientWalletId, amount, currency, description,
                SagaStep.COMPENSATE, SagaAction.COMPENSATE, reason, LocalDateTime.now()
        );
    }
}
