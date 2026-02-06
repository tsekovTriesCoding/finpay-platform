package com.finpay.payment.service;

import com.finpay.payment.dto.MoneyTransferRequest;
import com.finpay.payment.dto.MoneyTransferResponse;
import com.finpay.payment.entity.MoneyTransfer;
import com.finpay.payment.event.TransferSagaEvent;
import com.finpay.payment.exception.ResourceNotFoundException;
import com.finpay.payment.exception.TransferException;
import com.finpay.payment.kafka.WalletCommandProducer;
import com.finpay.payment.repository.MoneyTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Money Transfer Service implementing SAGA Choreography pattern for distributed transactions.
 * 
 * Fully event-driven architecture using Kafka:
 * - No REST calls to other services
 * - All wallet operations handled via Kafka commands/events
 * - Wallets are created automatically when users register (via user-events)
 * 
 * SAGA Steps (executed via Kafka events):
 * 1. RESERVE_FUNDS - Reserve funds in sender's wallet (wallet-service)
 * 2. DEDUCT_FUNDS - Deduct funds from sender's wallet (wallet-service)
 * 3. CREDIT_FUNDS - Credit funds to recipient's wallet (wallet-service)
 * 4. SEND_NOTIFICATION - Notify both parties (notification-service)
 * 5. COMPLETE - Mark transfer as complete
 * 
 * Compensation (if any step fails) is handled automatically in reverse order.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MoneyTransferService {

    private final MoneyTransferRepository transferRepository;
    private final WalletCommandProducer walletCommandProducer;
    private final TransferSagaEventProducer sagaEventProducer;

    /**
     * Initiate a money transfer using the SAGA Choreography pattern.
     * Creates the transfer record and starts the SAGA via Kafka.
     * Returns immediately with PROCESSING status - saga executes asynchronously.
     * 
     * Wallet validation happens asynchronously - if wallet doesn't exist or has
     * insufficient funds, the saga will fail and compensation will be triggered.
     */
    public MoneyTransferResponse initiateTransfer(UUID senderUserId, MoneyTransferRequest request) {
        log.info("Initiating transfer from {} to {} for amount {} {}",
                senderUserId, request.recipientUserId(), request.amount(), request.currency());

        // Validate sender and recipient are different
        if (senderUserId.equals(request.recipientUserId())) {
            throw new TransferException("Cannot transfer money to yourself");
        }

        // Validate amount is positive
        if (request.amount().signum() <= 0) {
            throw new TransferException("Transfer amount must be positive");
        }

        // Generate transaction reference
        String transactionReference = generateTransactionReference();

        // Create transfer record (wallet IDs will be populated by wallet-service responses)
        MoneyTransfer transfer = MoneyTransfer.builder()
                .transactionReference(transactionReference)
                .senderUserId(senderUserId)
                .recipientUserId(request.recipientUserId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .description(request.description())
                .status(MoneyTransfer.TransferStatus.PROCESSING)
                .sagaStatus(MoneyTransfer.SagaStatus.STARTED)
                .fundsReserved(false)
                .fundsDeducted(false)
                .fundsCredit(false)
                .notificationSent(false)
                .compensationRequired(false)
                .compensationCompleted(false)
                .build();

        MoneyTransfer savedTransfer = transferRepository.save(transfer);
        log.info("Transfer created with ID: {} and reference: {}", savedTransfer.getId(), transactionReference);

        // Publish saga initiate event for tracking/debugging
        TransferSagaEvent sagaEvent = TransferSagaEvent.initiate(
                savedTransfer.getId(),
                transactionReference,
                senderUserId,
                null,  // Wallet IDs not known yet - will be resolved by wallet-service
                request.recipientUserId(),
                null,
                request.amount(),
                request.currency(),
                request.description()
        );
        sagaEventProducer.sendSagaEvent(sagaEvent);

        // Start SAGA Step 1: Reserve funds via Kafka
        log.info("Starting SAGA via Kafka - Step 1: Reserve funds for transfer {}", savedTransfer.getId());
        walletCommandProducer.reserveFunds(
                savedTransfer.getId(),  // correlationId for saga tracking
                senderUserId,
                request.amount(),
                request.currency(),
                "Reserve funds for transfer: " + transactionReference
        );

        return MoneyTransferResponse.fromEntity(savedTransfer);
    }

    /**
     * Get transfer by ID.
     */
    @Transactional(readOnly = true)
    public MoneyTransferResponse getTransferById(UUID transferId) {
        MoneyTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));
        return MoneyTransferResponse.fromEntity(transfer);
    }

    /**
     * Get transfer by transaction reference.
     */
    @Transactional(readOnly = true)
    public MoneyTransferResponse getTransferByReference(String transactionReference) {
        MoneyTransfer transfer = transferRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found with reference: " + transactionReference));
        return MoneyTransferResponse.fromEntity(transfer);
    }

    /**
     * Get all transfers for a user (as sender or recipient).
     */
    @Transactional(readOnly = true)
    public Page<MoneyTransferResponse> getTransfersForUser(UUID userId, Pageable pageable) {
        return transferRepository.findByUserIdAsParticipant(userId, pageable)
                .map(MoneyTransferResponse::fromEntity);
    }

    private String generateTransactionReference() {
        return "TRF-" + System.currentTimeMillis() + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
