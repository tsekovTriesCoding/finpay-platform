package com.finpay.payment.shared.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.shared.event.WalletResponseEvent;
import com.finpay.payment.billpayment.BillPaymentService;
import com.finpay.payment.request.MoneyRequestService;
import com.finpay.payment.transfer.MoneyTransferService;
import com.finpay.payment.transfer.TransferSagaStepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Kafka consumer for wallet response events.
 * Implements SAGA choreography pattern — acts as a thin router that delegates
 * all entity state management to the owning domain service.
 * <p>
 * No repositories are accessed directly; each feature service owns its own data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletResponseConsumer {

    private final MoneyTransferService transferService;
    private final MoneyRequestService requestService;
    private final BillPaymentService billPaymentService;
    private final ObjectMapper kafkaObjectMapper;

    @KafkaListener(topics = "wallet-events", groupId = "payment-service-wallet-consumer")
    @Transactional
    public void handleWalletResponse(String message) {
        try {
            WalletResponseEvent event = kafkaObjectMapper.readValue(message, WalletResponseEvent.class);
            processWalletResponse(event);
        } catch (Exception e) {
            log.error("Failed to process wallet response: {}", message, e);
        }
    }

    private void processWalletResponse(WalletResponseEvent event) {
        log.info("Received wallet response: {} for correlationId: {}, success: {}",
                event.responseType(), event.correlationId(), event.success());

        UUID correlationId = event.correlationId();

        // Route to bill-payment saga if the correlationId matches a BillPayment
        if (billPaymentService.existsById(correlationId)) {
            routeToBillPaymentSaga(correlationId, event);
            return;
        }

        // Route to transfer saga
        if (transferService.findOptionalById(correlationId).isEmpty()) {
            log.warn("No transfer or bill payment found for correlationId: {}", correlationId);
            return;
        }

        if (!event.success()) {
            handleTransferFailure(correlationId, event);
            return;
        }

        switch (event.responseType()) {
            case FUNDS_RESERVED -> transferService.handleFundsReserved(correlationId, event.walletId());
            case FUNDS_DEDUCTED -> transferService.handleFundsDeducted(correlationId);
            case FUNDS_CREDITED -> handleTransferCredited(correlationId, event);
            case FUNDS_RELEASED -> transferService.handleFundsReleased(correlationId);
            case CREDIT_REVERSED -> transferService.handleCreditReversed(correlationId);
            case DEDUCTION_REVERSED -> transferService.handleDeductionReversed(correlationId);
            case OPERATION_FAILED -> handleTransferFailure(correlationId, event);
        }
    }

    /**
     * Handle successful funds-credited: complete transfer and linked request (if any).
     */
    private void handleTransferCredited(UUID correlationId, WalletResponseEvent event) {
        TransferSagaStepResult result = transferService.handleFundsCredited(correlationId, event.walletId());

        if (result.hasLinkedRequest()) {
            requestService.completeRequest(
                    result.sourceRequestId(),
                    result.senderWalletId(),
                    result.recipientWalletId()
            );
        }
    }

    /**
     * Handle transfer SAGA failure and fail the linked request (if any).
     */
    private void handleTransferFailure(UUID correlationId, WalletResponseEvent event) {
        TransferSagaStepResult result = transferService.handleSagaFailure(
                correlationId, event.failureReason());

        if (result.hasLinkedRequest()) {
            requestService.failRequest(result.sourceRequestId(), event.failureReason());
        }
    }

    // Bill Payment SAGA routing — already fully delegated to BillPaymentService

    private void routeToBillPaymentSaga(UUID correlationId, WalletResponseEvent event) {
        log.info("Routing wallet response {} to bill payment saga: {}",
                event.responseType(), correlationId);

        if (!event.success()) {
            billPaymentService.handleFailure(correlationId, event.failureReason());
            return;
        }

        switch (event.responseType()) {
            case FUNDS_RESERVED -> billPaymentService.handleFundsReserved(
                    correlationId, event.walletId());
            case FUNDS_DEDUCTED -> billPaymentService.handleFundsDeducted(correlationId);
            case FUNDS_RELEASED, DEDUCTION_REVERSED ->
                    billPaymentService.handleCompensated(correlationId);
            case OPERATION_FAILED ->
                    billPaymentService.handleFailure(correlationId, event.failureReason());
            default -> log.warn("Unhandled response type {} for bill payment {}",
                    event.responseType(), correlationId);
        }
    }
}
