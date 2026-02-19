package com.finpay.payment.shared.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.shared.event.WalletResponseEvent;
import com.finpay.outbox.idempotency.IdempotentConsumerService;
import com.finpay.payment.billpayment.BillPaymentService;
import com.finpay.payment.request.MoneyRequestService;
import com.finpay.payment.transfer.MoneyTransferService;
import com.finpay.payment.transfer.TransferSagaStepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Kafka consumer for wallet response events.
 * Implements SAGA choreography pattern - acts as a thin router that delegates
 * all entity state management to the owning domain service.
 *
 * Configured with non-blocking retries and DLT:
 * - 4 attempts with exponential backoff (1s, 2s, 4s)
 * - Failed SAGA messages go to wallet-events-dlt
 *
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
    private final IdempotentConsumerService idempotentConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = "wallet-events", groupId = "payment-service-wallet-consumer")
    @Transactional
    public void handleWalletResponse(String message,
                                     @Header(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws Exception {
        if (idempotentConsumer.isDuplicate(idempotencyKey)) {
            log.info("Duplicate wallet response detected, skipping: idempotencyKey={}", idempotencyKey);
            return;
        }

        WalletResponseEvent event = kafkaObjectMapper.readValue(message, WalletResponseEvent.class);
        processWalletResponse(event);

        idempotentConsumer.markProcessed(idempotencyKey, "wallet-response-consumer");
    }

    /**
     * Dead Letter Topic handler for wallet response events that failed all retry attempts.
     */
    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        log.error("DLT: Failed to process wallet response after all retries. Topic: {}, Key: {}, Value: {}, Error: {}",
                topic, record.key(), record.value(), errorMessage);
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
