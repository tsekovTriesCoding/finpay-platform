package com.finpay.payment.controller;

import com.finpay.payment.dto.MoneyRequestCreateDto;
import com.finpay.payment.dto.MoneyRequestResponse;
import com.finpay.payment.service.MoneyRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for money request operations.
 *
 * Endpoints:
 *   POST   /api/v1/payments/requests         – create a money request
 *   POST   /api/v1/payments/requests/{id}/approve  – payer approves
 *   POST   /api/v1/payments/requests/{id}/decline   – payer declines
 *   POST   /api/v1/payments/requests/{id}/cancel    – requester cancels
 *   GET    /api/v1/payments/requests/{id}     – get by ID
 *   GET    /api/v1/payments/requests/user/{userId}  – history for user
 *   GET    /api/v1/payments/requests/pending/incoming – pending for payer
 *   GET    /api/v1/payments/requests/pending/outgoing – pending by requester
 *   GET    /api/v1/payments/requests/pending/count    – badge count
 */
@RestController
@RequestMapping("/api/v1/payments/requests")
@RequiredArgsConstructor
public class MoneyRequestController {

    private final MoneyRequestService requestService;

    /**
     * Create a new money request (authenticated user = requester).
     */
    @PostMapping
    public ResponseEntity<MoneyRequestResponse> createRequest(
            @RequestHeader("X-User-Id") UUID requesterUserId,
            @Valid @RequestBody MoneyRequestCreateDto dto) {
        MoneyRequestResponse response = requestService.createRequest(requesterUserId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Payer approves a money request, triggering the transfer SAGA.
     */
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<MoneyRequestResponse> approveRequest(
            @RequestHeader("X-User-Id") UUID payerUserId,
            @PathVariable UUID requestId) {
        MoneyRequestResponse response = requestService.approveRequest(payerUserId, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Payer declines a money request.
     */
    @PostMapping("/{requestId}/decline")
    public ResponseEntity<MoneyRequestResponse> declineRequest(
            @RequestHeader("X-User-Id") UUID payerUserId,
            @PathVariable UUID requestId) {
        MoneyRequestResponse response = requestService.declineRequest(payerUserId, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Requester cancels their own pending request.
     */
    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<MoneyRequestResponse> cancelRequest(
            @RequestHeader("X-User-Id") UUID requesterUserId,
            @PathVariable UUID requestId) {
        MoneyRequestResponse response = requestService.cancelRequest(requesterUserId, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a single money request by ID.
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<MoneyRequestResponse> getRequestById(@PathVariable UUID requestId) {
        return ResponseEntity.ok(requestService.getRequestById(requestId));
    }

    /**
     * Get money request history for a user (as requester or payer).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<MoneyRequestResponse>> getRequestsForUser(
            @PathVariable UUID userId, Pageable pageable) {
        return ResponseEntity.ok(requestService.getRequestsForUser(userId, pageable));
    }

    /**
     * Get pending incoming requests (where authenticated user is the payer).
     */
    @GetMapping("/pending/incoming")
    public ResponseEntity<Page<MoneyRequestResponse>> getPendingIncoming(
            @RequestHeader("X-User-Id") UUID payerUserId, Pageable pageable) {
        return ResponseEntity.ok(requestService.getPendingRequestsForPayer(payerUserId, pageable));
    }

    /**
     * Get pending outgoing requests (where authenticated user is the requester).
     */
    @GetMapping("/pending/outgoing")
    public ResponseEntity<Page<MoneyRequestResponse>> getPendingOutgoing(
            @RequestHeader("X-User-Id") UUID requesterUserId, Pageable pageable) {
        return ResponseEntity.ok(requestService.getPendingRequestsByRequester(requesterUserId, pageable));
    }

    /**
     * Get count of pending incoming requests (for badge count).
     */
    @GetMapping("/pending/count")
    public ResponseEntity<Long> getPendingCount(
            @RequestHeader("X-User-Id") UUID payerUserId) {
        return ResponseEntity.ok(requestService.countPendingForPayer(payerUserId));
    }
}
