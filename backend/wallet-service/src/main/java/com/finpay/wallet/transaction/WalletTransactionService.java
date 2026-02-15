package com.finpay.wallet.transaction;

import com.finpay.wallet.transaction.dto.WalletTransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WalletTransactionService {

    private final WalletTransactionRepository transactionRepository;
    private final WalletTransactionMapper transactionMapper;

    @Transactional
    public void recordTransaction(UUID walletId, UUID userId, WalletTransaction.TransactionType type,
                                  BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                  String currency, String referenceId, String description) {
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(walletId).userId(userId).type(type)
                .amount(amount).balanceBefore(balanceBefore).balanceAfter(balanceAfter)
                .currency(currency).referenceId(referenceId).description(description)
                .status(WalletTransaction.TransactionStatus.COMPLETED).build();
        transactionRepository.save(transaction);
    }

    public Page<WalletTransactionResponse> getTransactionsByUserId(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(transactionMapper::toResponse);
    }

    public Page<WalletTransactionResponse> getTransactionsByWalletId(UUID walletId, Pageable pageable) {
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
                .map(transactionMapper::toResponse);
    }

    public List<WalletTransactionResponse> getRecentTransactions(UUID userId) {
        return transactionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(transactionMapper::toResponse).collect(Collectors.toList());
    }

    public List<WalletTransactionResponse> getTransactionsByReferenceId(String referenceId) {
        return transactionRepository.findByReferenceId(referenceId)
                .stream().map(transactionMapper::toResponse).collect(Collectors.toList());
    }
}
