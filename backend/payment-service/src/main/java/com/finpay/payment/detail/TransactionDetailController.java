package com.finpay.payment.detail;

import com.finpay.payment.detail.dto.TransactionDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for unified transaction detail views.
 * Provides a single consistent endpoint pattern for fetching
 * detailed transaction data including status timelines and available actions.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionDetailController {

    private final TransactionDetailService detailService;

    /**
     * Get detailed view of a money transfer including status timeline.
     */
    @GetMapping("/transfers/{transferId}")
    public ResponseEntity<TransactionDetailResponse> getTransferDetail(
            @PathVariable UUID transferId) {
        return ResponseEntity.ok(detailService.getTransferDetail(transferId));
    }

    /**
     * Get detailed view of a bill payment including status timeline.
     */
    @GetMapping("/bills/{billPaymentId}")
    public ResponseEntity<TransactionDetailResponse> getBillPaymentDetail(
            @PathVariable UUID billPaymentId) {
        return ResponseEntity.ok(detailService.getBillPaymentDetail(billPaymentId));
    }

    /**
     * Get detailed view of a money request including status timeline.
     */
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<TransactionDetailResponse> getMoneyRequestDetail(
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(detailService.getMoneyRequestDetail(requestId));
    }
}
