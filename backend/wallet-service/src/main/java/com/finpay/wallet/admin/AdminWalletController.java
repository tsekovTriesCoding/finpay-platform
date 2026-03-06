package com.finpay.wallet.admin;

import com.finpay.wallet.wallet.Wallet;
import com.finpay.wallet.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin endpoints for wallet management across the platform.
 * Security enforced at API Gateway level (AdminAuthFilter).
 */
@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
@Slf4j
public class AdminWalletController {

    private final AdminWalletService adminWalletService;

    /**
     * List all wallets across the platform with pagination and filtering.
     */
    @GetMapping
    public ResponseEntity<Page<WalletResponse>> listAllWallets(
            @RequestParam(required = false) Wallet.WalletStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<WalletResponse> wallets = adminWalletService.listAllWallets(status, sortBy, sortDir, page, size);
        return ResponseEntity.ok(wallets);
    }

    /**
     * Admin freeze a user's wallet.
     */
    @PostMapping("/user/{userId}/freeze")
    public ResponseEntity<WalletResponse> freezeWallet(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String adminIdStr) {
        log.info("Admin {} freezing wallet for user {}", adminIdStr, userId);
        WalletResponse response = adminWalletService.freezeWallet(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin unfreeze a user's wallet.
     */
    @PostMapping("/user/{userId}/unfreeze")
    public ResponseEntity<WalletResponse> unfreezeWallet(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String adminIdStr) {
        log.info("Admin {} unfreezing wallet for user {}", adminIdStr, userId);
        WalletResponse response = adminWalletService.unfreezeWallet(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get wallet metrics for admin KPI dashboard.
     */
    @GetMapping("/metrics")
    public ResponseEntity<AdminWalletMetrics> getWalletMetrics() {
        AdminWalletMetrics metrics = adminWalletService.getWalletMetrics();
        return ResponseEntity.ok(metrics);
    }
}
