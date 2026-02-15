package com.finpay.wallet.transaction;

import com.finpay.wallet.transaction.dto.WalletTransactionResponse;
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
public class WalletTransactionController {

    private final WalletTransactionService transactionService;

    @GetMapping("/user/{userId}/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @PathVariable UUID userId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByUserId(userId, pageable));
    }

    @GetMapping("/user/{userId}/transactions/recent")
    public ResponseEntity<List<WalletTransactionResponse>> getRecentTransactions(@PathVariable UUID userId) {
        return ResponseEntity.ok(transactionService.getRecentTransactions(userId));
    }

    @GetMapping("/transactions/reference/{referenceId}")
    public ResponseEntity<List<WalletTransactionResponse>> getTransactionsByReference(@PathVariable String referenceId) {
        return ResponseEntity.ok(transactionService.getTransactionsByReferenceId(referenceId));
    }
}
