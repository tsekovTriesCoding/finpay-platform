package com.finpay.payment.detail;

import com.finpay.payment.detail.dto.StatusTimelineEntry;
import com.finpay.payment.detail.dto.TransactionDetailResponse;
import com.finpay.payment.detail.dto.TransactionDetailResponse.TransactionType;
import com.finpay.payment.billpayment.BillPayment;
import com.finpay.payment.billpayment.BillPaymentService;
import com.finpay.payment.request.MoneyRequest;
import com.finpay.payment.request.MoneyRequestService;
import com.finpay.payment.transfer.MoneyTransfer;
import com.finpay.payment.transfer.MoneyTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service to build unified transaction detail views across all payment types.
 * Constructs receipt data, status timelines, and available actions for the
 * transaction detail sheet.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionDetailService {

    private final MoneyTransferService moneyTransferService;
    private final BillPaymentService billPaymentService;
    private final MoneyRequestService moneyRequestService;

    public TransactionDetailResponse getTransferDetail(UUID transferId) {
        MoneyTransfer t = moneyTransferService.getTransferEntity(transferId);
        return buildTransferDetail(t);
    }

    public TransactionDetailResponse getBillPaymentDetail(UUID billPaymentId) {
        BillPayment bp = billPaymentService.getBillPaymentEntity(billPaymentId);
        return buildBillPaymentDetail(bp);
    }

    public TransactionDetailResponse getMoneyRequestDetail(UUID requestId) {
        MoneyRequest mr = moneyRequestService.getMoneyRequestEntity(requestId);
        return buildMoneyRequestDetail(mr);
    }

    // -- Transfer detail builder --

    private TransactionDetailResponse buildTransferDetail(MoneyTransfer t) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("transferType", t.getTransferType().name());
        metadata.put("sagaStatus", t.getSagaStatus().name());
        if (t.getSourceRequestId() != null) {
            metadata.put("sourceRequestId", t.getSourceRequestId().toString());
        }
        metadata.put("fundsReserved", t.isFundsReserved());
        metadata.put("fundsDeducted", t.isFundsDeducted());
        metadata.put("fundsCredit", t.isFundsCredit());

        List<StatusTimelineEntry> timeline = buildTransferTimeline(t);
        List<String> actions = buildTransferActions(t);

        String title = t.getTransferType() == MoneyTransfer.TransferType.SEND
                ? "Money Transfer"
                : "Request Payment";

        return new TransactionDetailResponse(
                t.getId(),
                t.getTransactionReference(),
                TransactionType.TRANSFER,
                t.getSenderUserId(),
                t.getRecipientUserId(),
                t.getAmount(),
                t.getCurrency(),
                BigDecimal.ZERO,
                t.getAmount(),
                t.getStatus().name(),
                t.getFailureReason(),
                title,
                t.getTransactionReference(),
                t.getDescription(),
                metadata,
                timeline,
                actions,
                t.getCreatedAt(),
                t.getCompletedAt(),
                t.getUpdatedAt()
        );
    }

    private List<StatusTimelineEntry> buildTransferTimeline(MoneyTransfer t) {
        List<StatusTimelineEntry> timeline = new ArrayList<>();
        var status = t.getStatus();
        var saga = t.getSagaStatus();

        // Step 1: Created / Initiated
        timeline.add(StatusTimelineEntry.completed(
                "CREATED", "Transfer Initiated",
                "Transfer of " + t.getAmount() + " " + t.getCurrency() + " initiated",
                t.getCreatedAt()
        ));

        // Step 2: Funds Reserved
        if (t.isFundsReserved()) {
            timeline.add(StatusTimelineEntry.completed(
                    "FUNDS_RESERVED", "Funds Reserved",
                    "Funds held from sender wallet",
                    t.getUpdatedAt()
            ));
        } else if (isFailed(status)) {
            timeline.add(StatusTimelineEntry.failed(
                    "FUNDS_RESERVED", "Funds Reservation",
                    t.getFailureReason() != null ? t.getFailureReason() : "Failed to reserve funds",
                    t.getFailedAt()
            ));
        } else {
            timeline.add(isProcessing(status)
                    ? StatusTimelineEntry.current("FUNDS_RESERVED", "Reserving Funds", "Holding funds from sender wallet", null)
                    : StatusTimelineEntry.pending("FUNDS_RESERVED", "Reserve Funds", "Funds to be held from sender wallet"));
        }

        // Step 3: Funds Deducted
        if (t.isFundsDeducted()) {
            timeline.add(StatusTimelineEntry.completed(
                    "FUNDS_DEDUCTED", "Funds Deducted",
                    "Funds deducted from sender wallet",
                    t.getUpdatedAt()
            ));
        } else if (t.isFundsReserved() && isFailed(status)) {
            timeline.add(StatusTimelineEntry.failed(
                    "FUNDS_DEDUCTED", "Funds Deduction",
                    t.getFailureReason() != null ? t.getFailureReason() : "Failed to deduct funds",
                    t.getFailedAt()
            ));
        } else if (!isFailed(status)) {
            timeline.add(t.isFundsReserved() && isProcessing(status)
                    ? StatusTimelineEntry.current("FUNDS_DEDUCTED", "Deducting Funds", "Deducting funds from sender", null)
                    : StatusTimelineEntry.pending("FUNDS_DEDUCTED", "Deduct Funds", "Funds to be deducted from sender"));
        }

        // Step 4: Funds Credited
        if (t.isFundsCredit()) {
            timeline.add(StatusTimelineEntry.completed(
                    "FUNDS_CREDITED", "Funds Credited",
                    "Funds received in recipient wallet",
                    t.getCompletedAt() != null ? t.getCompletedAt() : t.getUpdatedAt()
            ));
        } else if (t.isFundsDeducted() && isFailed(status)) {
            timeline.add(StatusTimelineEntry.failed(
                    "FUNDS_CREDITED", "Funds Credit",
                    t.getFailureReason() != null ? t.getFailureReason() : "Failed to credit funds",
                    t.getFailedAt()
            ));
        } else if (!isFailed(status)) {
            timeline.add(t.isFundsDeducted() && isProcessing(status)
                    ? StatusTimelineEntry.current("FUNDS_CREDITED", "Crediting Funds", "Sending to recipient wallet", null)
                    : StatusTimelineEntry.pending("FUNDS_CREDITED", "Credit Funds", "Funds to be sent to recipient"));
        }

        // Step 5: Completed
        if (status == MoneyTransfer.TransferStatus.COMPLETED) {
            timeline.add(StatusTimelineEntry.completed(
                    "COMPLETED", "Transfer Complete",
                    "Transfer completed successfully",
                    t.getCompletedAt()
            ));
        } else if (!isFailed(status)) {
            timeline.add(StatusTimelineEntry.pending("COMPLETED", "Complete", "Transfer will be completed"));
        }

        // Compensation steps if applicable
        if (status == MoneyTransfer.TransferStatus.COMPENSATING) {
            timeline.add(StatusTimelineEntry.current(
                    "COMPENSATING", "Rolling Back",
                    "Reversing transaction due to failure",
                    t.getUpdatedAt()
            ));
        } else if (status == MoneyTransfer.TransferStatus.COMPENSATED) {
            timeline.add(StatusTimelineEntry.completed(
                    "COMPENSATED", "Rolled Back",
                    "Transaction fully reversed",
                    t.getUpdatedAt()
            ));
        }

        return timeline;
    }

    private List<String> buildTransferActions(MoneyTransfer t) {
        List<String> actions = new ArrayList<>();
        switch (t.getStatus()) {
            case PENDING -> actions.add("CANCEL");
            case COMPLETED -> actions.add("DISPUTE");
            case FAILED, COMPENSATED -> actions.add("RETRY");
            default -> {}
        }
        return actions;
    }

    // -- Bill Payment detail builder --

    private TransactionDetailResponse buildBillPaymentDetail(BillPayment bp) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("category", bp.getCategory().name());
        metadata.put("billerName", bp.getBillerName());
        metadata.put("billerCode", bp.getBillerCode());
        metadata.put("accountNumber", bp.getAccountNumber());
        if (bp.getAccountHolderName() != null) {
            metadata.put("accountHolderName", bp.getAccountHolderName());
        }
        if (bp.getBillerReference() != null) {
            metadata.put("billerReference", bp.getBillerReference());
        }
        metadata.put("sagaStatus", bp.getSagaStatus().name());
        metadata.put("fundsReserved", bp.isFundsReserved());
        metadata.put("fundsDeducted", bp.isFundsDeducted());
        metadata.put("billerConfirmed", bp.isBillerConfirmed());

        List<StatusTimelineEntry> timeline = buildBillPaymentTimeline(bp);
        List<String> actions = buildBillPaymentActions(bp);

        return new TransactionDetailResponse(
                bp.getId(),
                bp.getTransactionReference(),
                TransactionType.BILL_PAYMENT,
                bp.getUserId(),
                null,
                bp.getAmount(),
                bp.getCurrency(),
                bp.getProcessingFee(),
                bp.getTotalAmount(),
                bp.getStatus().name(),
                bp.getFailureReason(),
                "Bill Payment — " + bp.getBillerName(),
                bp.getCategory().name() + " • " + bp.getAccountNumber(),
                bp.getDescription(),
                metadata,
                timeline,
                actions,
                bp.getCreatedAt(),
                bp.getProcessedAt(),
                bp.getUpdatedAt()
        );
    }

    private List<StatusTimelineEntry> buildBillPaymentTimeline(BillPayment bp) {
        List<StatusTimelineEntry> timeline = new ArrayList<>();
        var status = bp.getStatus();

        // Step 1: Created
        timeline.add(StatusTimelineEntry.completed(
                "CREATED", "Payment Initiated",
                "Bill payment of " + bp.getAmount() + " " + bp.getCurrency() + " to " + bp.getBillerName(),
                bp.getCreatedAt()
        ));

        // Step 2: Funds Reserved
        if (bp.isFundsReserved()) {
            timeline.add(StatusTimelineEntry.completed(
                    "FUNDS_RESERVED", "Funds Reserved",
                    "Payment amount held in wallet",
                    bp.getUpdatedAt()
            ));
        } else if (isBillFailed(status)) {
            timeline.add(StatusTimelineEntry.failed(
                    "FUNDS_RESERVED", "Funds Reservation",
                    bp.getFailureReason() != null ? bp.getFailureReason() : "Failed to reserve funds",
                    bp.getFailedAt()
            ));
        } else {
            timeline.add(isBillProcessing(status)
                    ? StatusTimelineEntry.current("FUNDS_RESERVED", "Reserving Funds", "Holding payment amount", null)
                    : StatusTimelineEntry.pending("FUNDS_RESERVED", "Reserve Funds", "Payment amount to be held"));
        }

        // Step 3: Funds Deducted
        if (bp.isFundsDeducted()) {
            timeline.add(StatusTimelineEntry.completed(
                    "FUNDS_DEDUCTED", "Funds Deducted",
                    "Payment amount deducted from wallet",
                    bp.getUpdatedAt()
            ));
        } else if (bp.isFundsReserved() && isBillFailed(status)) {
            timeline.add(StatusTimelineEntry.failed(
                    "FUNDS_DEDUCTED", "Funds Deduction",
                    bp.getFailureReason() != null ? bp.getFailureReason() : "Failed to deduct",
                    bp.getFailedAt()
            ));
        } else if (!isBillFailed(status)) {
            timeline.add(bp.isFundsReserved() && isBillProcessing(status)
                    ? StatusTimelineEntry.current("FUNDS_DEDUCTED", "Processing Payment", "Deducting from wallet", null)
                    : StatusTimelineEntry.pending("FUNDS_DEDUCTED", "Process Payment", "Payment to be processed"));
        }

        // Step 4: Biller Confirmed
        if (bp.isBillerConfirmed()) {
            timeline.add(StatusTimelineEntry.completed(
                    "BILLER_CONFIRMED", "Biller Confirmed",
                    "Payment confirmed by " + bp.getBillerName()
                            + (bp.getBillerReference() != null ? " (Ref: " + bp.getBillerReference() + ")" : ""),
                    bp.getProcessedAt() != null ? bp.getProcessedAt() : bp.getUpdatedAt()
            ));
        } else if (bp.isFundsDeducted() && isBillFailed(status)) {
            timeline.add(StatusTimelineEntry.failed(
                    "BILLER_CONFIRMED", "Biller Confirmation",
                    bp.getFailureReason() != null ? bp.getFailureReason() : "Biller rejected payment",
                    bp.getFailedAt()
            ));
        } else if (!isBillFailed(status)) {
            timeline.add(bp.isFundsDeducted() && isBillProcessing(status)
                    ? StatusTimelineEntry.current("BILLER_CONFIRMED", "Awaiting Confirmation", "Waiting for biller response", null)
                    : StatusTimelineEntry.pending("BILLER_CONFIRMED", "Biller Confirmation", "Biller to confirm payment"));
        }

        // Step 5: Completed
        if (status == BillPayment.BillPaymentStatus.COMPLETED) {
            timeline.add(StatusTimelineEntry.completed(
                    "COMPLETED", "Payment Complete",
                    "Bill payment completed successfully",
                    bp.getProcessedAt()
            ));
        } else if (!isBillFailed(status)) {
            timeline.add(StatusTimelineEntry.pending("COMPLETED", "Complete", "Payment will be completed"));
        }

        // Compensation
        if (status == BillPayment.BillPaymentStatus.COMPENSATING) {
            timeline.add(StatusTimelineEntry.current("COMPENSATING", "Refunding", "Payment is being reversed", bp.getUpdatedAt()));
        } else if (status == BillPayment.BillPaymentStatus.COMPENSATED) {
            timeline.add(StatusTimelineEntry.completed("COMPENSATED", "Refunded", "Payment fully reversed", bp.getUpdatedAt()));
        }

        return timeline;
    }

    private List<String> buildBillPaymentActions(BillPayment bp) {
        List<String> actions = new ArrayList<>();
        switch (bp.getStatus()) {
            case PENDING -> actions.add("CANCEL");
            case COMPLETED -> actions.add("DISPUTE");
            case FAILED, COMPENSATED -> actions.add("RETRY");
            default -> {}
        }
        return actions;
    }

    // -- Money Request detail builder --

    private TransactionDetailResponse buildMoneyRequestDetail(MoneyRequest mr) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sagaStatus", mr.getSagaStatus() != null ? mr.getSagaStatus().name() : null);
        if (mr.getExpiresAt() != null) {
            metadata.put("expiresAt", mr.getExpiresAt().toString());
        }
        if (mr.getApprovedAt() != null) {
            metadata.put("approvedAt", mr.getApprovedAt().toString());
        }
        if (mr.getDeclinedAt() != null) {
            metadata.put("declinedAt", mr.getDeclinedAt().toString());
        }

        List<StatusTimelineEntry> timeline = buildMoneyRequestTimeline(mr);
        List<String> actions = buildMoneyRequestActions(mr);

        return new TransactionDetailResponse(
                mr.getId(),
                mr.getRequestReference(),
                TransactionType.MONEY_REQUEST,
                mr.getRequesterUserId(),
                mr.getPayerUserId(),
                mr.getAmount(),
                mr.getCurrency(),
                BigDecimal.ZERO,
                mr.getAmount(),
                mr.getStatus().name(),
                mr.getFailureReason(),
                "Money Request",
                mr.getRequestReference(),
                mr.getDescription(),
                metadata,
                timeline,
                actions,
                mr.getCreatedAt(),
                mr.getCompletedAt(),
                mr.getUpdatedAt()
        );
    }

    private List<StatusTimelineEntry> buildMoneyRequestTimeline(MoneyRequest mr) {
        List<StatusTimelineEntry> timeline = new ArrayList<>();
        var status = mr.getStatus();

        // Step 1: Created
        timeline.add(StatusTimelineEntry.completed(
                "CREATED", "Request Created",
                "Money request of " + mr.getAmount() + " " + mr.getCurrency() + " sent",
                mr.getCreatedAt()
        ));

        // Step 2: Approval
        if (status == MoneyRequest.RequestStatus.PENDING_APPROVAL) {
            timeline.add(StatusTimelineEntry.current(
                    "PENDING_APPROVAL", "Awaiting Approval",
                    "Waiting for payer to approve or decline",
                    null
            ));
        } else if (status == MoneyRequest.RequestStatus.DECLINED) {
            timeline.add(StatusTimelineEntry.failed(
                    "DECLINED", "Request Declined",
                    "The payer declined this request",
                    mr.getDeclinedAt()
            ));
        } else if (status == MoneyRequest.RequestStatus.CANCELLED) {
            timeline.add(StatusTimelineEntry.failed(
                    "CANCELLED", "Request Cancelled",
                    "This request was cancelled",
                    mr.getUpdatedAt()
            ));
        } else if (status == MoneyRequest.RequestStatus.EXPIRED) {
            timeline.add(StatusTimelineEntry.failed(
                    "EXPIRED", "Request Expired",
                    "This request has expired",
                    mr.getExpiresAt()
            ));
        } else {
            // Approved or further
            timeline.add(StatusTimelineEntry.completed(
                    "APPROVED", "Request Approved",
                    "Payer approved the request",
                    mr.getApprovedAt()
            ));
        }

        // Step 3: Processing (only if approved)
        if (mr.getApprovedAt() != null) {
            if (status == MoneyRequest.RequestStatus.PROCESSING) {
                timeline.add(StatusTimelineEntry.current(
                        "PROCESSING", "Processing Payment",
                        "Transfer is being processed",
                        null
                ));
            } else if (status == MoneyRequest.RequestStatus.COMPLETED) {
                timeline.add(StatusTimelineEntry.completed(
                        "PROCESSING", "Payment Processed",
                        "Transfer completed",
                        mr.getCompletedAt()
                ));
            } else if (status == MoneyRequest.RequestStatus.FAILED) {
                timeline.add(StatusTimelineEntry.failed(
                        "PROCESSING", "Payment Failed",
                        mr.getFailureReason() != null ? mr.getFailureReason() : "Transfer failed",
                        mr.getUpdatedAt()
                ));
            }
        }

        // Step 4: Completed
        if (status == MoneyRequest.RequestStatus.COMPLETED) {
            timeline.add(StatusTimelineEntry.completed(
                    "COMPLETED", "Request Fulfilled",
                    "Money received successfully",
                    mr.getCompletedAt()
            ));
        }

        return timeline;
    }

    private List<String> buildMoneyRequestActions(MoneyRequest mr) {
        List<String> actions = new ArrayList<>();
        switch (mr.getStatus()) {
            case PENDING_APPROVAL -> {
                actions.add("CANCEL");
                actions.add("APPROVE");
                actions.add("DECLINE");
            }
            case COMPLETED -> actions.add("DISPUTE");
            case FAILED -> actions.add("RETRY");
            default -> {}
        }
        return actions;
    }

    // Helpers

    private boolean isFailed(MoneyTransfer.TransferStatus status) {
        return status == MoneyTransfer.TransferStatus.FAILED
                || status == MoneyTransfer.TransferStatus.CANCELLED
                || status == MoneyTransfer.TransferStatus.COMPENSATING
                || status == MoneyTransfer.TransferStatus.COMPENSATED;
    }

    private boolean isProcessing(MoneyTransfer.TransferStatus status) {
        return status == MoneyTransfer.TransferStatus.PENDING
                || status == MoneyTransfer.TransferStatus.PROCESSING;
    }

    private boolean isBillFailed(BillPayment.BillPaymentStatus status) {
        return status == BillPayment.BillPaymentStatus.FAILED
                || status == BillPayment.BillPaymentStatus.CANCELLED
                || status == BillPayment.BillPaymentStatus.COMPENSATING
                || status == BillPayment.BillPaymentStatus.COMPENSATED;
    }

    private boolean isBillProcessing(BillPayment.BillPaymentStatus status) {
        return status == BillPayment.BillPaymentStatus.PENDING
                || status == BillPayment.BillPaymentStatus.PROCESSING;
    }
}
