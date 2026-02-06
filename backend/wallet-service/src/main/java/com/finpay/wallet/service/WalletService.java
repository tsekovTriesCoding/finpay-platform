package com.finpay.wallet.service;

import com.finpay.wallet.dto.WalletOperationResponse;
import com.finpay.wallet.dto.WalletResponse;
import com.finpay.wallet.entity.Wallet;
import com.finpay.wallet.entity.WalletTransaction;
import com.finpay.wallet.exception.InsufficientFundsException;
import com.finpay.wallet.exception.ResourceNotFoundException;
import com.finpay.wallet.exception.WalletException;
import com.finpay.wallet.repository.WalletRepository;
import com.finpay.wallet.repository.WalletTransactionRepository;
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
    private final WalletTransactionRepository transactionRepository;

    private static final BigDecimal DEFAULT_INITIAL_BALANCE = new BigDecimal("10000.00");
    private static final String DEFAULT_CURRENCY = "USD";

    /**
     * Get or create a wallet for a user.
     * Creates with default balance for demo purposes.
     */
    public WalletResponse getOrCreateWallet(UUID userId) {
        log.debug("Getting or creating wallet for user: {}", userId);
        
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> createWalletForUser(userId));
        
        return WalletResponse.fromEntity(wallet);
    }

    /**
     * Get wallet by user ID.
     */
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(UUID userId) {
        log.debug("Fetching wallet for user: {}", userId);
        
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
        
        return WalletResponse.fromEntity(wallet);
    }

    /**
     * Get wallet entity with lock for update operations.
     */
    public Wallet getWalletForUpdate(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
    }

    /**
     * Reserve funds for a pending transfer.
     * This is the first step in the SAGA pattern.
     */
    public WalletOperationResponse reserveFunds(UUID userId, BigDecimal amount, String referenceId) {
        log.info("Reserving {} funds for user: {}, reference: {}", amount, userId, referenceId);
        
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new WalletException("Wallet is not active");
        }
        
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds. Available: " + 
                    wallet.getAvailableBalance() + ", Required: " + amount);
        }
        
        boolean reserved = wallet.reserveFunds(amount);
        if (reserved) {
            walletRepository.save(wallet);
            
            // Record transaction
            recordTransaction(wallet, WalletTransaction.TransactionType.RESERVE,
                    amount, balanceBefore, wallet.getBalance(), referenceId, "Funds reserved for transfer");
            
            log.info("Successfully reserved {} for user: {}", amount, userId);
            return WalletOperationResponse.success(wallet.getId(), userId, "RESERVE",
                    amount, wallet.getBalance(), wallet.getAvailableBalance());
        }
        
        return WalletOperationResponse.failure(userId, "RESERVE", amount, "Failed to reserve funds");
    }

    /**
     * Release reserved funds (for failed/cancelled transfers).
     * This is a compensation step in the SAGA pattern.
     */
    public WalletOperationResponse releaseReservedFunds(UUID userId, BigDecimal amount, String referenceId) {
        log.info("Releasing {} reserved funds for user: {}, reference: {}", amount, userId, referenceId);
        
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        
        wallet.releaseReservedFunds(amount);
        walletRepository.save(wallet);
        
        // Record transaction
        recordTransaction(wallet, WalletTransaction.TransactionType.RELEASE_RESERVE,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Reserved funds released");
        
        log.info("Successfully released {} for user: {}", amount, userId);
        return WalletOperationResponse.success(wallet.getId(), userId, "RELEASE_RESERVE",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    /**
     * Deduct funds from sender wallet.
     * This is the second step in the SAGA pattern (after reservation).
     */
    public WalletOperationResponse deductFunds(UUID userId, BigDecimal amount, String referenceId) {
        log.info("Deducting {} from user: {}, reference: {}", amount, userId, referenceId);
        
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        
        if (wallet.getReservedBalance().compareTo(amount) < 0) {
            throw new WalletException("Reserved funds insufficient for deduction");
        }
        
        wallet.deductReservedFunds(amount);
        walletRepository.save(wallet);
        
        // Record transaction
        recordTransaction(wallet, WalletTransaction.TransactionType.DEBIT,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Funds deducted for transfer");
        
        log.info("Successfully deducted {} from user: {}", amount, userId);
        return WalletOperationResponse.success(wallet.getId(), userId, "DEBIT",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    /**
     * Credit funds to recipient wallet.
     * This is the third step in the SAGA pattern.
     */
    public WalletOperationResponse creditFunds(UUID userId, BigDecimal amount, String referenceId) {
        log.info("Crediting {} to user: {}, reference: {}", amount, userId, referenceId);
        
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new WalletException("Recipient wallet is not active");
        }
        
        wallet.creditFunds(amount);
        walletRepository.save(wallet);
        
        // Record transaction
        recordTransaction(wallet, WalletTransaction.TransactionType.CREDIT,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Funds credited from transfer");
        
        log.info("Successfully credited {} to user: {}", amount, userId);
        return WalletOperationResponse.success(wallet.getId(), userId, "CREDIT",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    /**
     * Reverse a credit operation (compensation for credit step).
     */
    public WalletOperationResponse reverseCredit(UUID userId, BigDecimal amount, String referenceId) {
        log.info("Reversing credit of {} from user: {}, reference: {}", amount, userId, referenceId);
        
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Cannot fully reverse credit. Balance: {}, Amount: {}", wallet.getBalance(), amount);
            return WalletOperationResponse.failure(userId, "REVERSE_CREDIT", amount,
                    "Insufficient balance for reversal");
        }
        
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        
        // Record transaction
        recordTransaction(wallet, WalletTransaction.TransactionType.DEBIT,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Credit reversed - compensation");
        
        log.info("Successfully reversed credit of {} from user: {}", amount, userId);
        return WalletOperationResponse.success(wallet.getId(), userId, "REVERSE_CREDIT",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    /**
     * Reverse a deduction (compensation for deduct step).
     */
    public WalletOperationResponse reverseDeduction(UUID userId, BigDecimal amount, String referenceId) {
        log.info("Reversing deduction: crediting {} back to user: {}, reference: {}", amount, userId, referenceId);
        
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        
        wallet.creditFunds(amount);
        walletRepository.save(wallet);
        
        // Record transaction
        recordTransaction(wallet, WalletTransaction.TransactionType.CREDIT,
                amount, balanceBefore, wallet.getBalance(), referenceId, "Deduction reversed - compensation");
        
        log.info("Successfully reversed deduction for user: {}", userId);
        return WalletOperationResponse.success(wallet.getId(), userId, "REVERSE_DEDUCTION",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    /**
     * Deposit funds into wallet (e.g., from external source).
     */
    public WalletOperationResponse deposit(UUID userId, BigDecimal amount, String referenceId, String description) {
        log.info("Depositing {} to user: {}", amount, userId);
        
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new WalletException("Wallet is not active");
        }
        
        wallet.creditFunds(amount);
        walletRepository.save(wallet);
        
        // Record transaction
        recordTransaction(wallet, WalletTransaction.TransactionType.DEPOSIT,
                amount, balanceBefore, wallet.getBalance(), referenceId, 
                description != null ? description : "Deposit");
        
        log.info("Successfully deposited {} to user: {}", amount, userId);
        return WalletOperationResponse.success(wallet.getId(), userId, "DEPOSIT",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    /**
     * Withdraw funds from wallet (e.g., to external account).
     */
    public WalletOperationResponse withdraw(UUID userId, BigDecimal amount, String referenceId, String description) {
        log.info("Withdrawing {} from user: {}", amount, userId);
        
        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getBalance();
        
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new WalletException("Wallet is not active");
        }
        
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds. Available: " + 
                    wallet.getAvailableBalance() + ", Required: " + amount);
        }
        
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        
        // Record transaction
        recordTransaction(wallet, WalletTransaction.TransactionType.WITHDRAWAL,
                amount, balanceBefore, wallet.getBalance(), referenceId,
                description != null ? description : "Withdrawal");
        
        log.info("Successfully withdrew {} from user: {}", amount, userId);
        return WalletOperationResponse.success(wallet.getId(), userId, "WITHDRAWAL",
                amount, wallet.getBalance(), wallet.getAvailableBalance());
    }

    /**
     * Freeze a wallet (e.g., for security reasons).
     */
    public WalletResponse freezeWallet(UUID userId) {
        log.info("Freezing wallet for user: {}", userId);
        
        Wallet wallet = getWalletForUpdate(userId);
        wallet.setStatus(Wallet.WalletStatus.FROZEN);
        walletRepository.save(wallet);
        
        log.info("Successfully froze wallet for user: {}", userId);
        return WalletResponse.fromEntity(wallet);
    }

    /**
     * Unfreeze a wallet.
     */
    public WalletResponse unfreezeWallet(UUID userId) {
        log.info("Unfreezing wallet for user: {}", userId);
        
        Wallet wallet = getWalletForUpdate(userId);
        if (wallet.getStatus() != Wallet.WalletStatus.FROZEN) {
            throw new WalletException("Wallet is not frozen");
        }
        wallet.setStatus(Wallet.WalletStatus.ACTIVE);
        walletRepository.save(wallet);
        
        log.info("Successfully unfroze wallet for user: {}", userId);
        return WalletResponse.fromEntity(wallet);
    }

    private Wallet createWalletForUser(UUID userId) {
        log.info("Creating new wallet for user: {} with initial balance: {}", userId, DEFAULT_INITIAL_BALANCE);
        
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(DEFAULT_INITIAL_BALANCE)
                .reservedBalance(BigDecimal.ZERO)
                .currency(DEFAULT_CURRENCY)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();
        
        Wallet savedWallet = walletRepository.save(wallet);
        
        // Record initial deposit transaction
        recordTransaction(savedWallet, WalletTransaction.TransactionType.DEPOSIT,
                DEFAULT_INITIAL_BALANCE, BigDecimal.ZERO, DEFAULT_INITIAL_BALANCE,
                null, "Initial wallet balance");
        
        return savedWallet;
    }

    private void recordTransaction(Wallet wallet, WalletTransaction.TransactionType type,
                                   BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                   String referenceId, String description) {
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(wallet.getId())
                .userId(wallet.getUserId())
                .type(type)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .currency(wallet.getCurrency())
                .referenceId(referenceId)
                .description(description)
                .status(WalletTransaction.TransactionStatus.COMPLETED)
                .build();
        transactionRepository.save(transaction);
    }
}
