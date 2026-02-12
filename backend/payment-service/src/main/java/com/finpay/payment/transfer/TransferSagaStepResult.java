package com.finpay.payment.transfer;

import java.util.UUID;

/**
 * Lightweight result returned by SAGA step handlers in {@link MoneyTransferService}.
 * Carries the information the SAGA consumer needs for cross-feature coordination
 * (e.g. completing or failing a linked {@code MoneyRequest}).
 */
public record TransferSagaStepResult(
        UUID sourceRequestId,
        UUID senderWalletId,
        UUID recipientWalletId
) {
    public boolean hasLinkedRequest() {
        return sourceRequestId != null;
    }

    static TransferSagaStepResult of(MoneyTransfer transfer) {
        return new TransferSagaStepResult(
                transfer.getSourceRequestId(),
                transfer.getSenderWalletId(),
                transfer.getRecipientWalletId()
        );
    }

    static TransferSagaStepResult none() {
        return new TransferSagaStepResult(null, null, null);
    }
}
