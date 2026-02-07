package com.finpay.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.entity.MoneyRequest;
import com.finpay.payment.entity.MoneyTransfer;
import com.finpay.payment.event.MoneyRequestEvent;
import com.finpay.payment.event.TransferSagaEvent;
import com.finpay.payment.event.WalletResponseEvent;
import com.finpay.payment.repository.MoneyRequestRepository;
import com.finpay.payment.repository.MoneyTransferRepository;
import com.finpay.payment.service.MoneyRequestEventProducer;
import com.finpay.payment.service.TransferSagaEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka consumer for wallet response events.
 * Implements SAGA choreography pattern - listens for wallet-service responses
 * and continues the transfer saga accordingly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletResponseConsumer {

    private final MoneyTransferRepository transferRepository;
    private final MoneyRequestRepository requestRepository;
    private final WalletCommandProducer walletCommandProducer;
    private final TransferSagaEventProducer sagaEventProducer;
    private final MoneyRequestEventProducer requestEventProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "wallet-events", groupId = "payment-service-wallet-consumer")
    @Transactional
    public void handleWalletResponse(String message) {
        try {
            WalletResponseEvent event = objectMapper.readValue(message, WalletResponseEvent.class);
            processWalletResponse(event);
        } catch (Exception e) {
            log.error("Failed to process wallet response: {}", message, e);
        }
    }

    private void processWalletResponse(WalletResponseEvent event) {
        log.info("Received wallet response: {} for correlationId: {}, success: {}",
                event.responseType(), event.correlationId(), event.success());

        UUID transferId = event.correlationId();
        MoneyTransfer transfer = transferRepository.findById(transferId).orElse(null);

        if (transfer == null) {
            log.warn("Transfer not found for correlationId: {}", transferId);
            return;
        }

        if (!event.success()) {
            handleFailedResponse(transfer, event);
            return;
        }

        // Continue saga based on response type
        switch (event.responseType()) {
            case FUNDS_RESERVED -> handleFundsReserved(transfer, event);
            case FUNDS_DEDUCTED -> handleFundsDeducted(transfer, event);
            case FUNDS_CREDITED -> handleFundsCredited(transfer, event);
            case FUNDS_RELEASED -> handleFundsReleased(transfer, event);
            case CREDIT_REVERSED -> handleCreditReversed(transfer, event);
            case DEDUCTION_REVERSED -> handleDeductionReversed(transfer, event);
            case OPERATION_FAILED -> handleFailedResponse(transfer, event);
        }
    }

    /**
     * Step 1 completed: Funds reserved, now deduct funds
     */
    private void handleFundsReserved(MoneyTransfer transfer, WalletResponseEvent event) {
        log.info("SAGA Step 1 completed: Funds reserved for transfer {}", transfer.getId());

        transfer.setFundsReserved(true);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.FUNDS_RESERVED);
        // Store sender's wallet ID from the event
        if (event.walletId() != null) {
            transfer.setSenderWalletId(event.walletId());
        }
        transferRepository.save(transfer);

        // Continue to step 2: Deduct funds from sender
        walletCommandProducer.deductFunds(
                transfer.getId(),
                transfer.getSenderUserId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                "Transfer deduction: " + transfer.getTransactionReference()
        );
    }

    /**
     * Step 2 completed: Funds deducted, now credit recipient
     */
    private void handleFundsDeducted(MoneyTransfer transfer, WalletResponseEvent event) {
        log.info("SAGA Step 2 completed: Funds deducted for transfer {}", transfer.getId());

        transfer.setFundsDeducted(true);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.FUNDS_DEDUCTED);
        transferRepository.save(transfer);

        // Continue to step 3: Credit funds to recipient
        walletCommandProducer.creditFunds(
                transfer.getId(),
                transfer.getRecipientUserId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                "Transfer credit from user " + transfer.getSenderUserId()
        );
    }

    /**
     * Step 3 completed: Funds credited, send notifications and complete
     */
    private void handleFundsCredited(MoneyTransfer transfer, WalletResponseEvent event) {
        log.info("SAGA Step 3 completed: Funds credited for transfer {}", transfer.getId());

        transfer.setFundsCredit(true);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.FUNDS_CREDITED);
        // Store recipient's wallet ID from the event
        if (event.walletId() != null) {
            transfer.setRecipientWalletId(event.walletId());
        }
        transferRepository.save(transfer);

        // Send notifications via Kafka
        TransferSagaEvent notificationEvent = new TransferSagaEvent(
                transfer.getId(),
                transfer.getTransactionReference(),
                transfer.getSenderUserId(),
                transfer.getSenderWalletId(),
                transfer.getRecipientUserId(),
                transfer.getRecipientWalletId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getDescription(),
                TransferSagaEvent.SagaStep.SEND_NOTIFICATION,
                TransferSagaEvent.SagaAction.EXECUTE,
                null,
                java.time.LocalDateTime.now()
        );
        sagaEventProducer.sendNotificationEvent(notificationEvent);

        // Complete the transfer
        completeTransfer(transfer);
    }

    /**
     * Compensation: Funds released after failed saga
     */
    private void handleFundsReleased(MoneyTransfer transfer, WalletResponseEvent event) {
        log.info("Compensation: Funds released for transfer {}", transfer.getId());
        transfer.setCompensationCompleted(true);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATED);
        transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATED);
        transferRepository.save(transfer);
    }

    /**
     * Compensation: Credit reversed
     */
    private void handleCreditReversed(MoneyTransfer transfer, WalletResponseEvent event) {
        log.info("Compensation: Credit reversed for transfer {}", transfer.getId());
        
        // If deduction was done, reverse it too
        if (transfer.isFundsDeducted()) {
            walletCommandProducer.reverseDeduction(
                    transfer.getId(),
                    transfer.getSenderUserId(),
                    transfer.getAmount(),
                    transfer.getCurrency(),
                    "Reversal of deduction for failed transfer: " + transfer.getTransactionReference()
            );
        } else if (transfer.isFundsReserved()) {
            // Just release the reservation
            walletCommandProducer.releaseFunds(
                    transfer.getId(),
                    transfer.getSenderUserId(),
                    transfer.getAmount(),
                    transfer.getCurrency(),
                    "Release funds for failed transfer: " + transfer.getTransactionReference()
            );
        } else {
            // No more compensation needed
            transfer.setCompensationCompleted(true);
            transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATED);
            transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATED);
            transferRepository.save(transfer);
        }
    }

    /**
     * Compensation: Deduction reversed (credit back to sender)
     */
    private void handleDeductionReversed(MoneyTransfer transfer, WalletResponseEvent event) {
        log.info("Compensation: Deduction reversed for transfer {}", transfer.getId());
        
        // Deduction reversed means we credited back, now release reservation
        if (transfer.isFundsReserved()) {
            walletCommandProducer.releaseFunds(
                    transfer.getId(),
                    transfer.getSenderUserId(),
                    transfer.getAmount(),
                    transfer.getCurrency(),
                    "Release funds after rollback: " + transfer.getTransactionReference()
            );
        } else {
            transfer.setCompensationCompleted(true);
            transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATED);
            transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATED);
            transferRepository.save(transfer);
        }
    }

    /**
     * Handle failed wallet operations - trigger compensation
     */
    private void handleFailedResponse(MoneyTransfer transfer, WalletResponseEvent event) {
        log.error("Wallet operation failed for transfer {}: {}", 
                transfer.getId(), event.failureReason());

        transfer.setFailureReason(event.failureReason());
        transfer.setStatus(MoneyTransfer.TransferStatus.FAILED);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.FAILED);
        transfer.setCompensationRequired(true);
        transfer.setFailedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        // If this transfer was triggered by a money request, fail the request too
        if (transfer.getSourceRequestId() != null) {
            requestRepository.findById(transfer.getSourceRequestId()).ifPresent(request -> {
                request.setStatus(MoneyRequest.RequestStatus.FAILED);
                request.setSagaStatus(MoneyRequest.SagaStatus.FAILED);
                request.setFailureReason(event.failureReason());
                request.setFailedAt(LocalDateTime.now());
                request.setCompensationRequired(true);
                requestRepository.save(request);

                requestEventProducer.publishRequestEvent(
                        MoneyRequestEvent.withFailure(
                                request.getId(), request.getRequestReference(),
                                request.getRequesterUserId(), request.getPayerUserId(),
                                request.getAmount(), request.getCurrency(),
                                request.getDescription(),
                                MoneyRequestEvent.EventType.REQUEST_FAILED,
                                event.failureReason()
                        )
                );
            });
        }

        // Start compensation based on what was already done
        startCompensation(transfer);
    }

    /**
     * Start compensation in reverse order
     */
    private void startCompensation(MoneyTransfer transfer) {
        log.warn("Starting compensation for failed transfer {}", transfer.getId());

        transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATING);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATING);
        transferRepository.save(transfer);

        if (transfer.isFundsCredit()) {
            // Step 3 was done - reverse credit first
            walletCommandProducer.reverseCredit(
                    transfer.getId(),
                    transfer.getRecipientUserId(),
                    transfer.getAmount(),
                    transfer.getCurrency(),
                    "Reversal for failed transfer: " + transfer.getTransactionReference()
            );
        } else if (transfer.isFundsDeducted()) {
            // Step 2 was done - reverse deduction
            walletCommandProducer.reverseDeduction(
                    transfer.getId(),
                    transfer.getSenderUserId(),
                    transfer.getAmount(),
                    transfer.getCurrency(),
                    "Reversal of deduction for failed transfer: " + transfer.getTransactionReference()
            );
        } else if (transfer.isFundsReserved()) {
            // Only step 1 was done - release funds
            walletCommandProducer.releaseFunds(
                    transfer.getId(),
                    transfer.getSenderUserId(),
                    transfer.getAmount(),
                    transfer.getCurrency(),
                    "Release funds for failed transfer: " + transfer.getTransactionReference()
            );
        } else {
            // Nothing was done, just mark as compensated
            transfer.setCompensationCompleted(true);
            transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPENSATED);
            transfer.setStatus(MoneyTransfer.TransferStatus.COMPENSATED);
            transferRepository.save(transfer);
        }
    }

    /**
     * Complete the transfer successfully.
     * If this transfer originated from a money request, also complete the request
     * and publish the REQUEST_COMPLETED event.
     */
    private void completeTransfer(MoneyTransfer transfer) {
        transfer.setNotificationSent(true);
        transfer.setStatus(MoneyTransfer.TransferStatus.COMPLETED);
        transfer.setSagaStatus(MoneyTransfer.SagaStatus.COMPLETED);
        transfer.setCompletedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        log.info("SAGA completed successfully for transfer {}", transfer.getId());

        // If this transfer was triggered by a money request, complete the request too
        if (transfer.getSourceRequestId() != null) {
            completeLinkedRequest(transfer);
        }
    }

    /**
     * Complete the linked MoneyRequest and publish notification events.
     */
    private void completeLinkedRequest(MoneyTransfer transfer) {
        requestRepository.findById(transfer.getSourceRequestId()).ifPresent(request -> {
            request.setStatus(MoneyRequest.RequestStatus.COMPLETED);
            request.setSagaStatus(MoneyRequest.SagaStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
            request.setNotificationSent(true);
            request.setFundsReserved(true);
            request.setFundsDeducted(true);
            request.setFundsCredited(true);
            request.setPayerWalletId(transfer.getSenderWalletId());
            request.setRequesterWalletId(transfer.getRecipientWalletId());
            requestRepository.save(request);

            requestEventProducer.publishRequestEvent(
                    MoneyRequestEvent.create(
                            request.getId(), request.getRequestReference(),
                            request.getRequesterUserId(), request.getPayerUserId(),
                            request.getAmount(), request.getCurrency(),
                            request.getDescription(),
                            MoneyRequestEvent.EventType.REQUEST_COMPLETED
                    )
            );

            log.info("Linked MoneyRequest {} completed via transfer {}",
                    request.getId(), transfer.getId());
        });
    }
}
