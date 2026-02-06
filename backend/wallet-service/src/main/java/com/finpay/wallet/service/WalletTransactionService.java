package com.finpay.wallet.service;

import com.finpay.wallet.dto.WalletTransactionResponse;
import com.finpay.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WalletTransactionService {

    private final WalletTransactionRepository transactionRepository;

    /**
     * Get paginated transaction history for a user.
     */
    public Page<WalletTransactionResponse> getTransactionsByUserId(UUID userId, Pageable pageable) {
        log.debug("Fetching transactions for user: {}", userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(WalletTransactionResponse::fromEntity);
    }

    /**
     * Get paginated transaction history for a wallet.
     */
    public Page<WalletTransactionResponse> getTransactionsByWalletId(UUID walletId, Pageable pageable) {
        log.debug("Fetching transactions for wallet: {}", walletId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
                .map(WalletTransactionResponse::fromEntity);
    }

    /**
     * Get recent transactions for a user.
     */
    public List<WalletTransactionResponse> getRecentTransactions(UUID userId) {
        log.debug("Fetching recent transactions for user: {}", userId);
        return transactionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(WalletTransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get transactions by reference ID.
     */
    public List<WalletTransactionResponse> getTransactionsByReferenceId(String referenceId) {
        log.debug("Fetching transactions for reference: {}", referenceId);
        return transactionRepository.findByReferenceId(referenceId)
                .stream()
                .map(WalletTransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
