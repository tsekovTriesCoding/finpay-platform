package com.finpay.payment.service;

import com.finpay.payment.dto.MoneyRequestCreateDto;
import com.finpay.payment.dto.MoneyRequestResponse;
import com.finpay.payment.entity.MoneyRequest;
import com.finpay.payment.entity.MoneyTransfer;
import com.finpay.payment.event.MoneyRequestEvent;
import com.finpay.payment.event.TransferSagaEvent;
import com.finpay.payment.exception.ResourceNotFoundException;
import com.finpay.payment.exception.TransferException;
import com.finpay.payment.kafka.WalletCommandProducer;
import com.finpay.payment.repository.MoneyRequestRepository;
import com.finpay.payment.repository.MoneyTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Money Request Service implementing the request-money flow.
 *
 * Phase 1 – Request creation (synchronous):
 *   Requester creates a request → payer receives notification via Kafka.
 *
 * Phase 2 – Approval triggers SAGA Choreography (event-driven, same as transfer):
 *   RESERVE_FUNDS (payer) → DEDUCT_FUNDS (payer) → CREDIT_FUNDS (requester)
 *   → SEND_NOTIFICATION → COMPLETE
 *
 * Compensation on failure follows the same reverse pattern as transfers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MoneyRequestService {

    private static final int REQUEST_EXPIRY_DAYS = 7;

    private final MoneyRequestRepository requestRepository;
    private final MoneyTransferRepository transferRepository;
    private final WalletCommandProducer walletCommandProducer;
    private final TransferSagaEventProducer sagaEventProducer;
    private final MoneyRequestEventProducer requestEventProducer;

    // Create Request

    /**
     * Create a money request from the authenticated user (requester) to a payer.
     */
    public MoneyRequestResponse createRequest(UUID requesterUserId, MoneyRequestCreateDto dto) {
        log.info("Creating money request: requester={} payer={} amount={} {}",
                requesterUserId, dto.payerUserId(), dto.amount(), dto.currency());

        if (requesterUserId.equals(dto.payerUserId())) {
            throw new TransferException("Cannot request money from yourself");
        }
        if (dto.amount().signum() <= 0) {
            throw new TransferException("Request amount must be positive");
        }

        String requestReference = generateRequestReference();

        MoneyRequest request = MoneyRequest.builder()
                .requestReference(requestReference)
                .requesterUserId(requesterUserId)
                .payerUserId(dto.payerUserId())
                .amount(dto.amount())
                .currency(dto.currency().toUpperCase())
                .description(dto.description())
                .status(MoneyRequest.RequestStatus.PENDING_APPROVAL)
                .sagaStatus(MoneyRequest.SagaStatus.NOT_STARTED)
                .fundsReserved(false)
                .fundsDeducted(false)
                .fundsCredited(false)
                .notificationSent(false)
                .compensationRequired(false)
                .compensationCompleted(false)
                .expiresAt(LocalDateTime.now().plusDays(REQUEST_EXPIRY_DAYS))
                .build();

        MoneyRequest saved = requestRepository.save(request);
        log.info("Money request created: id={} ref={}", saved.getId(), requestReference);

        // Notify the payer about the incoming request via Kafka
        requestEventProducer.publishRequestEvent(
                MoneyRequestEvent.create(
                        saved.getId(), requestReference,
                        requesterUserId, dto.payerUserId(),
                        dto.amount(), dto.currency().toUpperCase(),
                        dto.description(),
                        MoneyRequestEvent.EventType.REQUEST_CREATED
                )
        );

        return MoneyRequestResponse.fromEntity(saved);
    }

    // Approve Request - starts the SAGA

    /**
     * Payer approves the money request, triggering the transfer SAGA.
     *
     * Creates a MoneyTransfer record upfront so the existing WalletResponseConsumer
     * handles the entire SAGA - one unified code-path for both send-money and
     * request-money. The transfer correlationId (not the request ID) is used for
     * all wallet commands.
     */
    public MoneyRequestResponse approveRequest(UUID payerUserId, UUID requestId) {
        MoneyRequest request = findAndValidateForAction(requestId, payerUserId, true);

        log.info("Payer {} approving request {}", payerUserId, requestId);

        // 1. Create a MoneyTransfer record upfront
        String txRef = "TRF-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MoneyTransfer transfer = MoneyTransfer.builder()
                .transactionReference(txRef)
                .senderUserId(request.getPayerUserId())        // payer sends funds
                .recipientUserId(request.getRequesterUserId()) // requester receives
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "Payment for request " + request.getRequestReference())
                .transferType(MoneyTransfer.TransferType.REQUEST_PAYMENT)
                .sourceRequestId(request.getId())
                .status(MoneyTransfer.TransferStatus.PROCESSING)
                .sagaStatus(MoneyTransfer.SagaStatus.STARTED)
                .fundsReserved(false)
                .fundsDeducted(false)
                .fundsCredit(false)
                .notificationSent(false)
                .compensationRequired(false)
                .compensationCompleted(false)
                .build();

        transfer = transferRepository.save(transfer);
        log.info("Created MoneyTransfer {} for request {}", transfer.getId(), request.getId());

        // 2. Update the request status
        request.setStatus(MoneyRequest.RequestStatus.PROCESSING);
        request.setSagaStatus(MoneyRequest.SagaStatus.STARTED);
        request.setApprovedAt(LocalDateTime.now());
        requestRepository.save(request);

        // 3. Publish audit/trace event
        TransferSagaEvent sagaEvent = TransferSagaEvent.initiate(
                transfer.getId(),
                txRef,
                request.getPayerUserId(),
                null,
                request.getRequesterUserId(),
                null,
                request.getAmount(),
                request.getCurrency(),
                request.getDescription()
        );
        sagaEventProducer.sendSagaEvent(sagaEvent);

        // 4. Notify requester that their request was approved
        requestEventProducer.publishRequestEvent(
                MoneyRequestEvent.create(
                        request.getId(), request.getRequestReference(),
                        request.getRequesterUserId(), request.getPayerUserId(),
                        request.getAmount(), request.getCurrency(),
                        request.getDescription(),
                        MoneyRequestEvent.EventType.REQUEST_APPROVED
                )
        );

        // 5. Start SAGA Step 1 using the TRANSFER ID as correlation
        log.info("Starting request-payment SAGA – Step 1: Reserve funds from payer {} via transfer {}",
                request.getPayerUserId(), transfer.getId());

        walletCommandProducer.reserveFunds(
                transfer.getId(),   // ← transfer ID, NOT request ID
                request.getPayerUserId(),
                request.getAmount(),
                request.getCurrency(),
                "Reserve funds for payment request: " + request.getRequestReference()
        );

        return MoneyRequestResponse.fromEntity(request);
    }

    // Decline Request

    /**
     * Payer declines the money request.
     */
    public MoneyRequestResponse declineRequest(UUID payerUserId, UUID requestId) {
        MoneyRequest request = findAndValidateForAction(requestId, payerUserId, true);

        log.info("Payer {} declining request {}", payerUserId, requestId);

        request.setStatus(MoneyRequest.RequestStatus.DECLINED);
        request.setDeclinedAt(LocalDateTime.now());
        requestRepository.save(request);

        // Notify requester about decline
        requestEventProducer.publishRequestEvent(
                MoneyRequestEvent.create(
                        request.getId(), request.getRequestReference(),
                        request.getRequesterUserId(), request.getPayerUserId(),
                        request.getAmount(), request.getCurrency(),
                        request.getDescription(),
                        MoneyRequestEvent.EventType.REQUEST_DECLINED
                )
        );

        return MoneyRequestResponse.fromEntity(request);
    }

    // Cancel Request (by requester)

    /**
     * Requester cancels their own pending request.
     */
    public MoneyRequestResponse cancelRequest(UUID requesterUserId, UUID requestId) {
        MoneyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Money request not found: " + requestId));

        if (!request.getRequesterUserId().equals(requesterUserId)) {
            throw new TransferException("Only the requester can cancel this request");
        }
        if (request.getStatus() != MoneyRequest.RequestStatus.PENDING_APPROVAL) {
            throw new TransferException("Only pending requests can be cancelled. Current status: " + request.getStatus());
        }

        log.info("Requester {} cancelling request {}", requesterUserId, requestId);

        request.setStatus(MoneyRequest.RequestStatus.CANCELLED);
        requestRepository.save(request);

        // Notify payer that the request was cancelled
        requestEventProducer.publishRequestEvent(
                MoneyRequestEvent.create(
                        request.getId(), request.getRequestReference(),
                        request.getRequesterUserId(), request.getPayerUserId(),
                        request.getAmount(), request.getCurrency(),
                        request.getDescription(),
                        MoneyRequestEvent.EventType.REQUEST_CANCELLED
                )
        );

        return MoneyRequestResponse.fromEntity(request);
    }

    // Query methods

    @Transactional(readOnly = true)
    public MoneyRequestResponse getRequestById(UUID requestId) {
        MoneyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Money request not found: " + requestId));
        return MoneyRequestResponse.fromEntity(request);
    }

    @Transactional(readOnly = true)
    public Page<MoneyRequestResponse> getRequestsForUser(UUID userId, Pageable pageable) {
        return requestRepository.findByUserIdAsParticipant(userId, pageable)
                .map(MoneyRequestResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<MoneyRequestResponse> getPendingRequestsForPayer(UUID payerUserId, Pageable pageable) {
        return requestRepository.findPendingForPayer(payerUserId, pageable)
                .map(MoneyRequestResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<MoneyRequestResponse> getPendingRequestsByRequester(UUID requesterUserId, Pageable pageable) {
        return requestRepository.findPendingByRequester(requesterUserId, pageable)
                .map(MoneyRequestResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long countPendingForPayer(UUID payerUserId) {
        return requestRepository.countPendingForPayer(payerUserId);
    }

    // Helpers

    private MoneyRequest findAndValidateForAction(UUID requestId, UUID payerUserId, boolean mustBePayer) {
        MoneyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Money request not found: " + requestId));

        if (mustBePayer && !request.getPayerUserId().equals(payerUserId)) {
            throw new TransferException("Only the payer can perform this action on the request");
        }

        if (request.getStatus() != MoneyRequest.RequestStatus.PENDING_APPROVAL) {
            throw new TransferException(
                    "Request is no longer pending. Current status: " + request.getStatus());
        }

        // Check expiry
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
            request.setStatus(MoneyRequest.RequestStatus.EXPIRED);
            requestRepository.save(request);
            throw new TransferException("This money request has expired");
        }

        return request;
    }

    private String generateRequestReference() {
        return "REQ-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
