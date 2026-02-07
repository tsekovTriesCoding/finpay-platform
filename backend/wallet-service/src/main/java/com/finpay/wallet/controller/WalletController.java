package com.finpay.wallet.controller;

import com.finpay.wallet.dto.WalletOperationRequest;
import com.finpay.wallet.dto.WalletOperationResponse;
import com.finpay.wallet.dto.WalletResponse;
import com.finpay.wallet.dto.WalletTransactionResponse;
import com.finpay.wallet.service.WalletService;
import com.finpay.wallet.service.WalletTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletTransactionService transactionService;

    /**
     * Get wallet for a user.
     * Wallet must be created via Kafka events during user registration.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<WalletResponse> getWalletForUser(@PathVariable UUID userId) {
        WalletResponse response = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reserve funds (for SAGA pattern - step 1).
     */
    @PostMapping("/reserve")
    public ResponseEntity<WalletOperationResponse> reserveFunds(@Valid @RequestBody WalletOperationRequest request) {
        WalletOperationResponse response = walletService.reserveFunds(
                request.userId(), request.amount(), request.referenceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Release reserved funds (SAGA compensation).
     */
    @PostMapping("/release")
    public ResponseEntity<WalletOperationResponse> releaseReservedFunds(@Valid @RequestBody WalletOperationRequest request) {
        WalletOperationResponse response = walletService.releaseReservedFunds(
                request.userId(), request.amount(), request.referenceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Deduct funds (for SAGA pattern - step 2).
     */
    @PostMapping("/deduct")
    public ResponseEntity<WalletOperationResponse> deductFunds(@Valid @RequestBody WalletOperationRequest request) {
        WalletOperationResponse response = walletService.deductFunds(
                request.userId(), request.amount(), request.referenceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Credit funds (for SAGA pattern - step 3).
     */
    @PostMapping("/credit")
    public ResponseEntity<WalletOperationResponse> creditFunds(@Valid @RequestBody WalletOperationRequest request) {
        WalletOperationResponse response = walletService.creditFunds(
                request.userId(), request.amount(), request.referenceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Reverse credit (SAGA compensation).
     */
    @PostMapping("/reverse-credit")
    public ResponseEntity<WalletOperationResponse> reverseCredit(@Valid @RequestBody WalletOperationRequest request) {
        WalletOperationResponse response = walletService.reverseCredit(
                request.userId(), request.amount(), request.referenceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Reverse deduction (SAGA compensation).
     */
    @PostMapping("/reverse-deduction")
    public ResponseEntity<WalletOperationResponse> reverseDeduction(@Valid @RequestBody WalletOperationRequest request) {
        WalletOperationResponse response = walletService.reverseDeduction(
                request.userId(), request.amount(), request.referenceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Deposit funds into wallet.
     */
    @PostMapping("/deposit")
    public ResponseEntity<WalletOperationResponse> deposit(@Valid @RequestBody WalletOperationRequest request) {
        WalletOperationResponse response = walletService.deposit(
                request.userId(), request.amount(), request.referenceId(), request.description());
        return ResponseEntity.ok(response);
    }

    /**
     * Withdraw funds from wallet.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<WalletOperationResponse> withdraw(@Valid @RequestBody WalletOperationRequest request) {
        WalletOperationResponse response = walletService.withdraw(
                request.userId(), request.amount(), request.referenceId(), request.description());
        return ResponseEntity.ok(response);
    }

    /**
     * Freeze wallet.
     */
    @PostMapping("/user/{userId}/freeze")
    public ResponseEntity<WalletResponse> freezeWallet(@PathVariable UUID userId) {
        WalletResponse response = walletService.freezeWallet(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Unfreeze wallet.
     */
    @PostMapping("/user/{userId}/unfreeze")
    public ResponseEntity<WalletResponse> unfreezeWallet(@PathVariable UUID userId) {
        WalletResponse response = walletService.unfreezeWallet(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction history for a user.
     */
    @GetMapping("/user/{userId}/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WalletTransactionResponse> transactions = transactionService.getTransactionsByUserId(userId, pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get recent transactions for a user.
     */
    @GetMapping("/user/{userId}/transactions/recent")
    public ResponseEntity<List<WalletTransactionResponse>> getRecentTransactions(@PathVariable UUID userId) {
        List<WalletTransactionResponse> transactions = transactionService.getRecentTransactions(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get transactions by reference ID.
     */
    @GetMapping("/transactions/reference/{referenceId}")
    public ResponseEntity<List<WalletTransactionResponse>> getTransactionsByReference(@PathVariable String referenceId) {
        List<WalletTransactionResponse> transactions = transactionService.getTransactionsByReferenceId(referenceId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Wallet Service is running");
    }
}
