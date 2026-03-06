package com.finpay.payment.admin;

import java.math.BigDecimal;

public record AdminTransactionMetrics(
        long totalTransfers,
        long completedTransfers,
        long failedTransfers,
        long pendingTransfers,
        BigDecimal totalTransferVolume,
        long totalBillPayments,
        long completedBillPayments,
        long failedBillPayments,
        BigDecimal totalBillPaymentVolume,
        long totalMoneyRequests,
        long pendingMoneyRequests,
        long completedMoneyRequests,
        long declinedMoneyRequests,
        long flaggedTransactions
) {
}
