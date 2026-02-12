package com.finpay.payment.transfer;

import com.finpay.payment.transfer.dto.MoneyTransferRequest;
import com.finpay.payment.transfer.dto.MoneyTransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/transfers")
@RequiredArgsConstructor
public class MoneyTransferController {

    private final MoneyTransferService transferService;

    /**
     * Initiate a money transfer from the sender to a recipient.
     * Uses SAGA pattern for distributed transaction management.
     */
    @PostMapping
    public ResponseEntity<MoneyTransferResponse> initiateTransfer(
            @RequestHeader("X-User-Id") UUID senderUserId,
            @Valid @RequestBody MoneyTransferRequest request) {
        MoneyTransferResponse response = transferService.initiateTransfer(senderUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get transfer by ID.
     */
    @GetMapping("/{transferId}")
    public ResponseEntity<MoneyTransferResponse> getTransferById(@PathVariable UUID transferId) {
        MoneyTransferResponse response = transferService.getTransferById(transferId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transfer by transaction reference.
     */
    @GetMapping("/reference/{transactionReference}")
    public ResponseEntity<MoneyTransferResponse> getTransferByReference(
            @PathVariable String transactionReference) {
        MoneyTransferResponse response = transferService.getTransferByReference(transactionReference);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all transfers for a user (as sender or recipient).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<MoneyTransferResponse>> getTransfersForUser(
            @PathVariable UUID userId,
            Pageable pageable) {
        Page<MoneyTransferResponse> response = transferService.getTransfersForUser(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
