package com.finpay.payment.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin endpoints for transaction monitoring across the platform.
 * Security enforced at API Gateway level (AdminAuthFilter).
 */
@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@Slf4j
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;

    /**
     * List all transactions across the platform.
     * Supports filtering by type (TRANSFER, BILL_PAYMENT, MONEY_REQUEST), status, pagination, sorting.
     */
    @GetMapping
    public ResponseEntity<Page<AdminTransactionResponse>> listAllTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AdminTransactionResponse> transactions = adminTransactionService.listAllTransactions(
                type, status, sortBy, sortDir, page, size);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get transaction metrics for admin KPI dashboard.
     */
    @GetMapping("/metrics")
    public ResponseEntity<AdminTransactionMetrics> getTransactionMetrics() {
        AdminTransactionMetrics metrics = adminTransactionService.getTransactionMetrics();
        return ResponseEntity.ok(metrics);
    }
}
