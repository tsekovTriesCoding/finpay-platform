package com.finpay.wallet.admin;

import com.finpay.wallet.wallet.WalletRepository;
import com.finpay.wallet.wallet.WalletService;
import com.finpay.wallet.wallet.dto.WalletResponse;
import com.finpay.wallet.wallet.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminWalletService {

    private final WalletRepository walletRepository;
    private final WalletService walletService;

    /**
     * List all wallets with server-side pagination, sorting, and filtering.
     */
    public Page<WalletResponse> listAllWallets(Wallet.WalletStatus status,
                                                String sortBy, String sortDir,
                                                int page, int size) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mapSortField(sortBy));
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        Page<Wallet> wallets;
        if (status != null) {
            wallets = walletRepository.findByStatus(status, pageable);
        } else {
            wallets = walletRepository.findAll(pageable);
        }

        return wallets.map(this::toWalletResponse);
    }

    /**
     * Admin freeze wallet (delegates to existing WalletService).
     */
    @Transactional
    public WalletResponse freezeWallet(UUID userId) {
        walletService.freezeWallet(userId);
        return walletService.getWalletByUserId(userId);
    }

    /**
     * Admin unfreeze wallet (delegates to existing WalletService).
     */
    @Transactional
    public WalletResponse unfreezeWallet(UUID userId) {
        walletService.unfreezeWallet(userId);
        return walletService.getWalletByUserId(userId);
    }

    /**
     * Get wallet metrics for admin dashboard.
     */
    public AdminWalletMetrics getWalletMetrics() {
        long totalWallets = walletRepository.count();
        long activeWallets = walletRepository.countByStatus(Wallet.WalletStatus.ACTIVE);
        long frozenWallets = walletRepository.countByStatus(Wallet.WalletStatus.FROZEN);
        long closedWallets = walletRepository.countByStatus(Wallet.WalletStatus.CLOSED);

        BigDecimal totalBalance = walletRepository.findAll().stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminWalletMetrics(totalWallets, activeWallets, frozenWallets, closedWallets, totalBalance);
    }

    private WalletResponse toWalletResponse(Wallet w) {
        return new WalletResponse(
                w.getId(), w.getUserId(), w.getBalance(), w.getReservedBalance(),
                w.getAvailableBalance(), w.getCurrency(), w.getStatus(),
                w.getPlan(), w.getDailyTransactionLimit(), w.getMonthlyTransactionLimit(),
                w.getSpendTracker() != null ? w.getSpendTracker().getDailySpent() : BigDecimal.ZERO,
                w.getSpendTracker() != null ? w.getSpendTracker().getMonthlySpent() : BigDecimal.ZERO,
                w.getDailyTransactionLimit().subtract(
                        w.getSpendTracker() != null ? w.getSpendTracker().getDailySpent() : BigDecimal.ZERO),
                w.getMonthlyTransactionLimit().subtract(
                        w.getSpendTracker() != null ? w.getSpendTracker().getMonthlySpent() : BigDecimal.ZERO),
                w.getMaxVirtualCards(), w.getMultiCurrencyEnabled(), w.getApiAccessEnabled(),
                w.getCreatedAt(), w.getUpdatedAt()
        );
    }

    private String mapSortField(String sortBy) {
        if (sortBy == null) return "createdAt";
        return switch (sortBy) {
            case "balance" -> "balance";
            case "status" -> "status";
            case "date", "createdAt" -> "createdAt";
            default -> "createdAt";
        };
    }
}
