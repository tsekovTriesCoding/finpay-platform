package com.finpay.payment.controller;

import com.finpay.payment.dto.BillPaymentRequest;
import com.finpay.payment.dto.BillPaymentResponse;
import com.finpay.payment.entity.BillPayment;
import com.finpay.payment.service.BillPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/bills")
@RequiredArgsConstructor
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    /**
     * Initiate a new bill payment. Starts the saga workflow.
     */
    @PostMapping
    public ResponseEntity<BillPaymentResponse> payBill(
            @Valid @RequestBody BillPaymentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        // Prefer the header user-id set by the gateway / auth filter
        BillPaymentRequest effective = request;
        if (headerUserId != null && request.userId() == null) {
            effective = new BillPaymentRequest(
                    UUID.fromString(headerUserId),
                    request.category(), request.billerName(), request.billerCode(),
                    request.accountNumber(), request.accountHolderName(),
                    request.amount(), request.currency(), request.description());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billPaymentService.initiateBillPayment(effective));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillPaymentResponse> getBillPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(billPaymentService.getBillPayment(id));
    }

    @GetMapping("/reference/{ref}")
    public ResponseEntity<BillPaymentResponse> getBillByReference(@PathVariable String ref) {
        return ResponseEntity.ok(billPaymentService.getBillPaymentByReference(ref));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<BillPaymentResponse>> getUserBillPayments(
            @PathVariable UUID userId, Pageable pageable) {
        return ResponseEntity.ok(billPaymentService.getBillPaymentsByUser(userId, pageable));
    }

    @GetMapping("/user/{userId}/category/{category}")
    public ResponseEntity<Page<BillPaymentResponse>> getUserBillsByCategory(
            @PathVariable UUID userId,
            @PathVariable BillPayment.BillCategory category,
            Pageable pageable) {
        return ResponseEntity.ok(billPaymentService.getBillPaymentsByCategory(userId, category, pageable));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BillPaymentResponse> cancelBillPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(billPaymentService.cancelBillPayment(id));
    }

    /**
     * Returns the list of bill categories (used by the frontend picker).
     */
    @GetMapping("/categories")
    public ResponseEntity<BillPayment.BillCategory[]> getCategories() {
        return ResponseEntity.ok(billPaymentService.getAvailableCategories());
    }
}
