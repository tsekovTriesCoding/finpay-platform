package com.finpay.payment.admin;

import com.finpay.payment.billpayment.BillPayment;
import com.finpay.payment.billpayment.BillPaymentRepository;
import com.finpay.payment.request.MoneyRequest;
import com.finpay.payment.request.MoneyRequestRepository;
import com.finpay.payment.transfer.MoneyTransfer;
import com.finpay.payment.transfer.MoneyTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminTransactionService {

    private final MoneyTransferRepository transferRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final MoneyRequestRepository moneyRequestRepository;

    /**
     * Get all transactions across the platform with type filter.
     */
    public Page<AdminTransactionResponse> listAllTransactions(
            String type, String status, String sortBy, String sortDir, int page, int size) {

        if ("TRANSFER".equalsIgnoreCase(type) || type == null) {
            return listTransfers(status, sortBy, sortDir, page, size);
        }
        if ("BILL_PAYMENT".equalsIgnoreCase(type)) {
            return listBillPayments(status, sortBy, sortDir, page, size);
        }
        if ("MONEY_REQUEST".equalsIgnoreCase(type)) {
            return listMoneyRequests(status, sortBy, sortDir, page, size);
        }

        // Default: return transfers
        return listTransfers(status, sortBy, sortDir, page, size);
    }

    private Page<AdminTransactionResponse> listTransfers(
            String status, String sortBy, String sortDir, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mapSortField(sortBy));
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        Page<MoneyTransfer> transfers;
        if (status != null && !status.isBlank()) {
            transfers = transferRepository.findByStatusPaged(
                    MoneyTransfer.TransferStatus.valueOf(status), pageable);
        } else {
            transfers = transferRepository.findAll(pageable);
        }

        return transfers.map(t -> new AdminTransactionResponse(
                t.getId(),
                "TRANSFER",
                t.getTransactionReference(),
                t.getSenderUserId(),
                t.getRecipientUserId(),
                t.getAmount(),
                t.getCurrency(),
                t.getStatus().name(),
                t.getDescription(),
                false, // flagged — to be extended with flagging feature
                null,
                t.getCreatedAt(),
                t.getUpdatedAt()
        ));
    }

    private Page<AdminTransactionResponse> listBillPayments(
            String status, String sortBy, String sortDir, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mapSortField(sortBy));
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        Page<BillPayment> bills;
        if (status != null && !status.isBlank()) {
            bills = billPaymentRepository.findByStatusPaged(
                    BillPayment.BillPaymentStatus.valueOf(status), pageable);
        } else {
            bills = billPaymentRepository.findAll(pageable);
        }

        return bills.map(b -> new AdminTransactionResponse(
                b.getId(),
                "BILL_PAYMENT",
                b.getTransactionReference(),
                b.getUserId(),
                null,
                b.getAmount(),
                b.getCurrency(),
                b.getStatus().name(),
                b.getBillerName() + " - " + b.getCategory(),
                false,
                null,
                b.getCreatedAt(),
                b.getUpdatedAt()
        ));
    }

    private Page<AdminTransactionResponse> listMoneyRequests(
            String status, String sortBy, String sortDir, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mapSortField(sortBy));
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        Page<MoneyRequest> requests;
        if (status != null && !status.isBlank()) {
            requests = moneyRequestRepository.findByStatusPaged(
                    MoneyRequest.RequestStatus.valueOf(status), pageable);
        } else {
            requests = moneyRequestRepository.findAll(pageable);
        }

        return requests.map(r -> new AdminTransactionResponse(
                r.getId(),
                "MONEY_REQUEST",
                r.getRequestReference(),
                r.getRequesterUserId(),
                r.getPayerUserId(),
                r.getAmount(),
                r.getCurrency(),
                r.getStatus().name(),
                r.getDescription(),
                false,
                null,
                r.getCreatedAt(),
                r.getUpdatedAt()
        ));
    }

    /**
     * Get transaction metrics for the admin dashboard.
     */
    public AdminTransactionMetrics getTransactionMetrics() {
        long totalTransfers = transferRepository.count();
        long completedTransfers = transferRepository.findByStatus(MoneyTransfer.TransferStatus.COMPLETED).size();
        long failedTransfers = transferRepository.findByStatus(MoneyTransfer.TransferStatus.FAILED).size();
        long pendingTransfers = transferRepository.findByStatus(MoneyTransfer.TransferStatus.PENDING).size();

        BigDecimal totalTransferVolume = transferRepository.findAll().stream()
                .filter(t -> t.getStatus() == MoneyTransfer.TransferStatus.COMPLETED)
                .map(MoneyTransfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalBillPayments = billPaymentRepository.count();
        long completedBillPayments = billPaymentRepository.countByStatus(BillPayment.BillPaymentStatus.COMPLETED);
        long failedBillPayments = billPaymentRepository.countByStatus(BillPayment.BillPaymentStatus.FAILED);

        BigDecimal totalBillPaymentVolume = billPaymentRepository.findAll().stream()
                .filter(b -> b.getStatus() == BillPayment.BillPaymentStatus.COMPLETED)
                .map(BillPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalMoneyRequests = moneyRequestRepository.count();
        long pendingMoneyRequests = moneyRequestRepository.countByStatus(MoneyRequest.RequestStatus.PENDING_APPROVAL);
        long completedMoneyRequests = moneyRequestRepository.countByStatus(MoneyRequest.RequestStatus.COMPLETED);
        long declinedMoneyRequests = moneyRequestRepository.countByStatus(MoneyRequest.RequestStatus.DECLINED);

        return new AdminTransactionMetrics(
                totalTransfers, completedTransfers, failedTransfers, pendingTransfers, totalTransferVolume,
                totalBillPayments, completedBillPayments, failedBillPayments, totalBillPaymentVolume,
                totalMoneyRequests, pendingMoneyRequests, completedMoneyRequests, declinedMoneyRequests,
                0 // flagged — to be extended
        );
    }

    private String mapSortField(String sortBy) {
        if (sortBy == null) return "createdAt";
        return switch (sortBy) {
            case "date", "createdAt" -> "createdAt";
            case "amount" -> "amount";
            case "status" -> "status";
            default -> "createdAt";
        };
    }
}
