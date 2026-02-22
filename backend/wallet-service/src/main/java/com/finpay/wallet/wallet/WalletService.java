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
                .orElseGet(() -> createWalletForUser(userId, Wallet.AccountPlan.STARTER));
        return walletMapper.toResponse(wallet);
    }

    /**
     * Creates or retrieves a wallet with plan-specific configuration.
     * Called when a user registers with a specific subscription plan.
     */
    public WalletResponse getOrCreateWalletWithPlan(UUID userId, String plan) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet.AccountPlan accountPlan = resolveAccountPlan(plan);
                    return createWalletForUser(userId, accountPlan);
                });
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

    /**
     * Upgrades a wallet's plan and applies the new plan's limits and features.
     * Does NOT change the current balance — only limits and feature flags are updated.
     */
    public void upgradePlan(UUID userId, String newPlanName) {
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            log.warn("No wallet found for user {} during plan upgrade, skipping", userId);
            return;
        }

        Wallet.AccountPlan newPlan = resolveAccountPlan(newPlanName);

        if (wallet.getPlan() == newPlan) {
            log.info("Wallet for user {} already on plan {}, skipping", userId, newPlan);
            return;
        }

        Wallet.AccountPlan previousPlan = wallet.getPlan();
        PlanConfiguration config = PlanConfiguration.forPlan(newPlan);

        wallet.setPlan(newPlan);
        wallet.setDailyTransactionLimit(config.dailyLimit());
        wallet.setMonthlyTransactionLimit(config.monthlyLimit());
        wallet.setMaxVirtualCards(config.maxVirtualCards());
        wallet.setMultiCurrencyEnabled(config.multiCurrencyEnabled());
        wallet.setApiAccessEnabled(config.apiAccessEnabled());

        walletRepository.save(wallet);
        log.info("Upgraded wallet for user {} from {} to {} - daily limit: {}, monthly limit: {}",
                userId, previousPlan, newPlan, config.dailyLimit(), config.monthlyLimit());
    }

    private Wallet createWalletForUser(UUID userId, Wallet.AccountPlan plan) {
        PlanConfiguration config = PlanConfiguration.forPlan(plan);
        
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(config.initialBalance())
                .reservedBalance(BigDecimal.ZERO)
                .currency(DEFAULT_CURRENCY)
                .status(Wallet.WalletStatus.ACTIVE)
                .plan(plan)
                .dailyTransactionLimit(config.dailyLimit())
                .monthlyTransactionLimit(config.monthlyLimit())
                .maxVirtualCards(config.maxVirtualCards())
                .multiCurrencyEnabled(config.multiCurrencyEnabled())
                .apiAccessEnabled(config.apiAccessEnabled())
                .build();
        
        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Created {} wallet for user: {} with daily limit: {}, monthly limit: {}",
                plan, userId, config.dailyLimit(), config.monthlyLimit());
        
        recordTransaction(savedWallet, WalletTransaction.TransactionType.DEPOSIT,
                config.initialBalance(), BigDecimal.ZERO, config.initialBalance(),
                null, "Initial " + plan.name().toLowerCase() + " plan wallet balance");
        return savedWallet;
    }

    private Wallet.AccountPlan resolveAccountPlan(String plan) {
        if (plan == null || plan.isBlank()) {
            return Wallet.AccountPlan.STARTER;
        }
        try {
            return Wallet.AccountPlan.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown plan '{}', defaulting to STARTER", plan);
            return Wallet.AccountPlan.STARTER;
        }
    }

    /**
     * Plan-specific wallet configuration.
     * Defines initial balance, transaction limits, and feature flags per plan tier.
     */
    private record PlanConfiguration(
            BigDecimal initialBalance,
            BigDecimal dailyLimit,
            BigDecimal monthlyLimit,
            int maxVirtualCards,
            boolean multiCurrencyEnabled,
            boolean apiAccessEnabled
    ) {
        static PlanConfiguration forPlan(Wallet.AccountPlan plan) {
            return switch (plan) {
                case STARTER -> new PlanConfiguration(
                        new BigDecimal("1000.00"),      // $1,000 welcome balance
                        new BigDecimal("500.00"),       // $500/day limit
                        new BigDecimal("5000.00"),      // $5,000/month limit
                        1,                               // 1 virtual card
                        false,                           // No multi-currency
                        false                            // No API access
                );
                case PRO -> new PlanConfiguration(
                        new BigDecimal("10000.00"),     // $10,000 welcome balance
                        new BigDecimal("10000.00"),     // $10,000/day limit
                        new BigDecimal("100000.00"),    // $100,000/month limit
                        10,                              // 10 virtual cards
                        true,                            // Multi-currency enabled
                        true                             // API access enabled
                );
                case ENTERPRISE -> new PlanConfiguration(
                        new BigDecimal("50000.00"),     // $50,000 welcome balance
                        new BigDecimal("1000000.00"),   // $1M/day limit
                        new BigDecimal("10000000.00"),  // $10M/month limit
                        Integer.MAX_VALUE,               // Unlimited virtual cards
                        true,                            // Multi-currency enabled
                        true                             // API access enabled
                );
            };
        }
    }

    private void recordTransaction(Wallet wallet, WalletTransaction.TransactionType type,
                                   BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                   String referenceId, String description) {
        transactionService.recordTransaction(wallet.getId(), wallet.getUserId(), type,
                amount, balanceBefore, balanceAfter, wallet.getCurrency(), referenceId, description);
    }
}
