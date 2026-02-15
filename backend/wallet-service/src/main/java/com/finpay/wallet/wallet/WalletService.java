package com.finpay.wallet.wallet;

import com.finpay.wallet.shared.exception.InsufficientFundsException;
import com.finpay.wallet.shared.exception.ResourceNotFoundException;
import com.finpay.wallet.shared.exception.WalletException;
import com.finpay.wallet.transaction.WalletTransaction;
import com.finpay.wallet.transaction.WalletTransactionService;
import com.finpay.wallet.wallet.dto.WalletOperationResponse;
import com.finpay.wallet.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionService transactionService;
    private final WalletMapper walletMapper;

    private static final BigDecimal DEFAULT_INITIAL_BALANCE = new BigDecimal("10000.00");
    private static final String DEFAULT_CURRENCY = "USD";

    public WalletResponse getOrCreateWallet(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> createWalletForUser(userId));
        return walletMapper.toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
        return walletMapper.toResponse(wallet);
    }

    public Wallet getWalletForUpdate(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
    }

    public WalletOperationResponse reserveFunds(UUID userId, BigDecimal amount, String referenceId) {
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE)
            throw new WalletException("Wallet is not active");
        if (wallet.getAvailableBalance().compareTo(amount) < 0)
            throw new InsufficientFundsException("Insufficient funds. Available: " +
                    wallet.getAvailableBalance() + ", Required: " + amount);
        boolean reserved = wallet.reserveFunds(amount);
        if (reserved) {
            walletRepository.save(wallet);
            recordTransaction(wallet, WalletTransaction.TransactionType.RESERVE,
                    amount, balanceBefore, wallet.getBalance(), referenceId, "Funds reserved for transfer");
            return WalletOperationResponse.success(wallet.getId(), userId, "RESERVE",
                    amount, wallet.getBalance(), wallet.getAvailableBalance());
        }
        return WalletOperationResponse.failure(userId, "RESERVE", amount, "Failed to reserve funds");
    }

    public WalletOperationResponse releaseReservedFunds(UUID userId, BigDecimal amount, String referenceId) {
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.releaseReservedFunds(amount);
        walletRepository.save(wallet);
        recordTransaction(wallet, WalletTransaction.TransactionType.RELEASE_RESERVE,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Reserved funds released");
        return WalletOperationResponse.success(wallet.getId(), userId, "RELEASE_RESERVE",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    public WalletOperationResponse deductFunds(UUID userId, BigDecimal amount, String referenceId) {
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        if (wallet.getReservedBalance().compareTo(amount) < 0)
            throw new WalletException("Reserved funds insufficient for deduction");
        wallet.deductReservedFunds(amount);
        walletRepository.save(wallet);
        recordTransaction(wallet, WalletTransaction.TransactionType.DEBIT,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Funds deducted for transfer");
        return WalletOperationResponse.success(wallet.getId(), userId, "DEBIT",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    public WalletOperationResponse creditFunds(UUID userId, BigDecimal amount, String referenceId) {
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE)
            throw new WalletException("Recipient wallet is not active");
        wallet.creditFunds(amount);
        walletRepository.save(wallet);
        recordTransaction(wallet, WalletTransaction.TransactionType.CREDIT,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Funds credited from transfer");
        return WalletOperationResponse.success(wallet.getId(), userId, "CREDIT",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    public WalletOperationResponse reverseCredit(UUID userId, BigDecimal amount, String referenceId) {
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        if (wallet.getBalance().compareTo(amount) < 0)
            return WalletOperationResponse.failure(userId, "REVERSE_CREDIT", amount,
                    "Insufficient balance for reversal");
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet, WalletTransaction.TransactionType.DEBIT,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Credit reversed - compensation");
        return WalletOperationResponse.success(wallet.getId(), userId, "REVERSE_CREDIT",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    public WalletOperationResponse reverseDeduction(UUID userId, BigDecimal amount, String referenceId) {
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.creditFunds(amount);
        walletRepository.save(wallet);
        recordTransaction(wallet, WalletTransaction.TransactionType.CREDIT,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Deduction reversed - compensation");
        return WalletOperationResponse.success(wallet.getId(), userId, "REVERSE_DEDUCTION",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    public WalletOperationResponse deposit(UUID userId, BigDecimal amount, String referenceId, String description) {
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE)
            throw new WalletException("Wallet is not active");
        wallet.creditFunds(amount);
        walletRepository.save(wallet);
        recordTransaction(wallet, WalletTransaction.TransactionType.DEPOSIT,
                amount, balanceBefore, wallet.getBalance(), referenceId,
                description != null ? description : "Deposit");
        return WalletOperationResponse.success(wallet.getId(), userId, "DEPOSIT",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    public WalletOperationResponse withdraw(UUID userId, BigDecimal amount, String referenceId, String description) {
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE)
            throw new WalletException("Wallet is not active");
        if (wallet.getAvailableBalance().compareTo(amount) < 0)
            throw new InsufficientFundsException("Insufficient funds. Available: " +
                    wallet.getAvailableBalance() + ", Required: " + amount);
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet, WalletTransaction.TransactionType.WITHDRAWAL,
                amount, balanceBefore, wallet.getBalance(), referenceId,
                description != null ? description : "Withdrawal");
        return WalletOperationResponse.success(wallet.getId(), userId, "WITHDRAWAL",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    public WalletResponse freezeWallet(UUID userId) {
        Wallet wallet = getWalletForUpdate(userId);
        wallet.setStatus(Wallet.WalletStatus.FROZEN);
        walletRepository.save(wallet);
        return walletMapper.toResponse(wallet);
    }

    public WalletResponse unfreezeWallet(UUID userId) {
        Wallet wallet = getWalletForUpdate(userId);
        if (wallet.getStatus() != Wallet.WalletStatus.FROZEN)
            throw new WalletException("Wallet is not frozen");
        wallet.setStatus(Wallet.WalletStatus.ACTIVE);
        walletRepository.save(wallet);
        return walletMapper.toResponse(wallet);
    }

    public void closeWalletForUser(UUID userId) {
        walletRepository.findByUserId(userId).ifPresent(wallet -> {
            if (wallet.getStatus() != Wallet.WalletStatus.CLOSED) {
                wallet.setStatus(Wallet.WalletStatus.CLOSED);
                walletRepository.save(wallet);
            }
        });
    }

    private Wallet createWalletForUser(UUID userId) {
        Wallet wallet = Wallet.builder()
                .userId(userId).balance(DEFAULT_INITIAL_BALANCE)
                .reservedBalance(BigDecimal.ZERO).currency(DEFAULT_CURRENCY)
                .status(Wallet.WalletStatus.ACTIVE).build();
        Wallet savedWallet = walletRepository.save(wallet);
        recordTransaction(savedWallet, WalletTransaction.TransactionType.DEPOSIT,
                DEFAULT_INITIAL_BALANCE, BigDecimal.ZERO, DEFAULT_INITIAL_BALANCE,
                null, "Initial wallet balance");
        return savedWallet;
    }

    private void recordTransaction(Wallet wallet, WalletTransaction.TransactionType type,
                                   BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                   String referenceId, String description) {
        transactionService.recordTransaction(wallet.getId(), wallet.getUserId(), type,
                amount, balanceBefore, balanceAfter, wallet.getCurrency(), referenceId, description);
    }
}
