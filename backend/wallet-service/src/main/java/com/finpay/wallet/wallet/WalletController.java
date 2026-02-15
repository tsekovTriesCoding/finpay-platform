package com.finpay.wallet.wallet;

import com.finpay.wallet.wallet.dto.WalletOperationRequest;
import com.finpay.wallet.wallet.dto.WalletOperationResponse;
import com.finpay.wallet.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<WalletResponse> getWalletForUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @PostMapping("/reserve")
    public ResponseEntity<WalletOperationResponse> reserveFunds(@Valid @RequestBody WalletOperationRequest request) {
        return ResponseEntity.ok(walletService.reserveFunds(request.userId(), request.amount(), request.referenceId()));
    }

    @PostMapping("/release")
    public ResponseEntity<WalletOperationResponse> releaseReservedFunds(@Valid @RequestBody WalletOperationRequest request) {
        return ResponseEntity.ok(walletService.releaseReservedFunds(request.userId(), request.amount(), request.referenceId()));
    }

    @PostMapping("/deduct")
    public ResponseEntity<WalletOperationResponse> deductFunds(@Valid @RequestBody WalletOperationRequest request) {
        return ResponseEntity.ok(walletService.deductFunds(request.userId(), request.amount(), request.referenceId()));
    }

    @PostMapping("/credit")
    public ResponseEntity<WalletOperationResponse> creditFunds(@Valid @RequestBody WalletOperationRequest request) {
        return ResponseEntity.ok(walletService.creditFunds(request.userId(), request.amount(), request.referenceId()));
    }

    @PostMapping("/reverse-credit")
    public ResponseEntity<WalletOperationResponse> reverseCredit(@Valid @RequestBody WalletOperationRequest request) {
        return ResponseEntity.ok(walletService.reverseCredit(request.userId(), request.amount(), request.referenceId()));
    }

    @PostMapping("/reverse-deduction")
    public ResponseEntity<WalletOperationResponse> reverseDeduction(@Valid @RequestBody WalletOperationRequest request) {
        return ResponseEntity.ok(walletService.reverseDeduction(request.userId(), request.amount(), request.referenceId()));
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletOperationResponse> deposit(@Valid @RequestBody WalletOperationRequest request) {
        return ResponseEntity.ok(walletService.deposit(request.userId(), request.amount(), request.referenceId(), request.description()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletOperationResponse> withdraw(@Valid @RequestBody WalletOperationRequest request) {
        return ResponseEntity.ok(walletService.withdraw(request.userId(), request.amount(), request.referenceId(), request.description()));
    }

    @PostMapping("/user/{userId}/freeze")
    public ResponseEntity<WalletResponse> freezeWallet(@PathVariable UUID userId) {
        return ResponseEntity.ok(walletService.freezeWallet(userId));
    }

    @PostMapping("/user/{userId}/unfreeze")
    public ResponseEntity<WalletResponse> unfreezeWallet(@PathVariable UUID userId) {
        return ResponseEntity.ok(walletService.unfreezeWallet(userId));
    }
}
