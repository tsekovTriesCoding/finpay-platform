package com.finpay.wallet.wallet;

import com.finpay.wallet.shared.exception.InsufficientFundsException;
import com.finpay.wallet.shared.exception.ResourceNotFoundException;
import com.finpay.wallet.shared.exception.TransactionLimitExceededException;
import com.finpay.wallet.shared.exception.WalletException;
import com.finpay.wallet.transaction.WalletTransaction;
import com.finpay.wallet.transaction.WalletTransactionService;
import com.finpay.wallet.wallet.dto.WalletOperationResponse;
import com.finpay.wallet.wallet.dto.WalletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Unit Tests")
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionService transactionService;
    @Mock private WalletMapper walletMapper;
    @Mock private WalletCacheService walletCacheService;

    @InjectMocks private WalletService walletService;

    private UUID userId;
    private Wallet activeWallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(new BigDecimal("1000.00"))
                .reservedBalance(BigDecimal.ZERO)
                .currency("USD")
                .status(Wallet.WalletStatus.ACTIVE)
                .plan(Wallet.AccountPlan.STARTER)
                .dailyTransactionLimit(new BigDecimal("500.00"))
                .monthlyTransactionLimit(new BigDecimal("5000.00"))
                .spendTracker(new SpendTracker())
                .maxVirtualCards(1)
                .multiCurrencyEnabled(false)
                .apiAccessEnabled(false)
                .build();
    }

    @Nested
    @DisplayName("Get Or Create Wallet")
    class GetOrCreateWalletTests {

        @Test
        @DisplayName("should return existing wallet")
        void shouldReturnExistingWallet() {
            WalletResponse mockResponse = mock(WalletResponse.class);
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(activeWallet));
            when(walletMapper.toResponse(activeWallet)).thenReturn(mockResponse);

            WalletResponse response = walletService.getOrCreateWallet(userId);

            assertThat(response).isEqualTo(mockResponse);
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create new wallet when none exists")
        void shouldCreateNewWalletWhenNoneExists() {
            WalletResponse mockResponse = mock(WalletResponse.class);
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);
            when(walletMapper.toResponse(any(Wallet.class))).thenReturn(mockResponse);

            WalletResponse response = walletService.getOrCreateWallet(userId);

            assertThat(response).isEqualTo(mockResponse);
            verify(walletRepository).save(any(Wallet.class));
        }
    }

    @Nested
    @DisplayName("Reserve Funds")
    class ReserveFundsTests {

        @Test
        @DisplayName("should reserve funds successfully")
        void shouldReserveFundsSuccessfully() {
            BigDecimal amount = new BigDecimal("100.00");
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

            WalletOperationResponse response = walletService.reserveFunds(userId, amount, "ref-1");

            assertThat(response.success()).isTrue();
            assertThat(response.operation()).isEqualTo("RESERVE");
            verify(transactionService).recordTransaction(
                    eq(activeWallet.getId()), eq(userId),
                    eq(WalletTransaction.TransactionType.RESERVE),
                    eq(amount), any(), any(), eq("USD"), eq("ref-1"), anyString()
            );
        }

        @Test
        @DisplayName("should throw InsufficientFundsException when balance too low")
        void shouldThrowWhenInsufficientFunds() {
            BigDecimal amount = new BigDecimal("2000.00"); // More than balance
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.reserveFunds(userId, amount, "ref-1"))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient funds");
        }

        @Test
        @DisplayName("should throw WalletException when wallet is not active")
        void shouldThrowWhenWalletNotActive() {
            activeWallet.setStatus(Wallet.WalletStatus.FROZEN);
            BigDecimal amount = new BigDecimal("100.00");
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.reserveFunds(userId, amount, "ref-1"))
                    .isInstanceOf(WalletException.class)
                    .hasMessage("Wallet is not active");
        }

        @Test
        @DisplayName("should throw when daily limit exceeded")
        void shouldThrowWhenDailyLimitExceeded() {
            // Set daily spent close to limit
            activeWallet.getSpendTracker().setDailySpent(new BigDecimal("450.00"));
            BigDecimal amount = new BigDecimal("100.00"); // Would exceed 500 limit

            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.reserveFunds(userId, amount, "ref-1"))
                    .isInstanceOf(TransactionLimitExceededException.class);
        }

        @Test
        @DisplayName("should throw when monthly limit exceeded")
        void shouldThrowWhenMonthlyLimitExceeded() {
            activeWallet.setDailyTransactionLimit(new BigDecimal("100000.00")); // Set high daily to pass daily check
            activeWallet.getSpendTracker().setMonthlySpent(new BigDecimal("4950.00"));
            BigDecimal amount = new BigDecimal("100.00"); // Would exceed 5000 limit

            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.reserveFunds(userId, amount, "ref-1"))
                    .isInstanceOf(TransactionLimitExceededException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.reserveFunds(userId, new BigDecimal("100.00"), "ref-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Deduct Funds")
    class DeductFundsTests {

        @Test
        @DisplayName("should deduct reserved funds successfully")
        void shouldDeductFundsSuccessfully() {
            BigDecimal amount = new BigDecimal("100.00");
            activeWallet.setReservedBalance(new BigDecimal("100.00"));
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

            WalletOperationResponse response = walletService.deductFunds(userId, amount, "ref-1");

            assertThat(response.success()).isTrue();
            assertThat(response.operation()).isEqualTo("DEBIT");
        }

        @Test
        @DisplayName("should throw when reserved funds insufficient for deduction")
        void shouldThrowWhenReservedFundsInsufficient() {
            BigDecimal amount = new BigDecimal("200.00");
            activeWallet.setReservedBalance(new BigDecimal("100.00")); // Less than amount
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.deductFunds(userId, amount, "ref-1"))
                    .isInstanceOf(WalletException.class)
                    .hasMessage("Reserved funds insufficient for deduction");
        }
    }

    @Nested
    @DisplayName("Credit Funds")
    class CreditFundsTests {

        @Test
        @DisplayName("should credit funds successfully")
        void shouldCreditFundsSuccessfully() {
            BigDecimal amount = new BigDecimal("200.00");
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

            WalletOperationResponse response = walletService.creditFunds(userId, amount, "ref-1");

            assertThat(response.success()).isTrue();
            assertThat(response.operation()).isEqualTo("CREDIT");
        }

        @Test
        @DisplayName("should throw when recipient wallet is not active")
        void shouldThrowWhenRecipientWalletNotActive() {
            activeWallet.setStatus(Wallet.WalletStatus.FROZEN);
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.creditFunds(userId, new BigDecimal("100.00"), "ref-1"))
                    .isInstanceOf(WalletException.class)
                    .hasMessage("Recipient wallet is not active");
        }
    }

    @Nested
    @DisplayName("Deposit")
    class DepositTests {

        @Test
        @DisplayName("should deposit funds successfully")
        void shouldDepositSuccessfully() {
            BigDecimal amount = new BigDecimal("500.00");
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

            WalletOperationResponse response = walletService.deposit(userId, amount, "dep-1", "Salary deposit");

            assertThat(response.success()).isTrue();
            assertThat(response.operation()).isEqualTo("DEPOSIT");
        }

        @Test
        @DisplayName("should throw when wallet is not active for deposit")
        void shouldThrowWhenWalletNotActiveForDeposit() {
            activeWallet.setStatus(Wallet.WalletStatus.CLOSED);
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.deposit(userId, new BigDecimal("100.00"), "dep-1", null))
                    .isInstanceOf(WalletException.class)
                    .hasMessage("Wallet is not active");
        }
    }

    @Nested
    @DisplayName("Withdraw")
    class WithdrawTests {

        @Test
        @DisplayName("should withdraw funds successfully")
        void shouldWithdrawSuccessfully() {
            BigDecimal amount = new BigDecimal("200.00");
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

            WalletOperationResponse response = walletService.withdraw(userId, amount, "wd-1", "ATM withdrawal");

            assertThat(response.success()).isTrue();
            assertThat(response.operation()).isEqualTo("WITHDRAWAL");
        }

        @Test
        @DisplayName("should throw when insufficient funds for withdrawal")
        void shouldThrowWhenInsufficientFundsForWithdrawal() {
            BigDecimal amount = new BigDecimal("2000.00"); // More than balance
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.withdraw(userId, amount, "wd-1", null))
                    .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("should throw when wallet is not active for withdrawal")
        void shouldThrowWhenWalletNotActiveForWithdrawal() {
            activeWallet.setStatus(Wallet.WalletStatus.FROZEN);
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.withdraw(userId, new BigDecimal("100.00"), "wd-1", null))
                    .isInstanceOf(WalletException.class);
        }
    }

    @Nested
    @DisplayName("Freeze/Unfreeze Wallet")
    class FreezeUnfreezeTests {

        @Test
        @DisplayName("should freeze active wallet")
        void shouldFreezeActiveWallet() {
            WalletResponse mockResponse = mock(WalletResponse.class);
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);
            when(walletMapper.toResponse(any(Wallet.class))).thenReturn(mockResponse);

            WalletResponse response = walletService.freezeWallet(userId);

            assertThat(response).isNotNull();
            assertThat(activeWallet.getStatus()).isEqualTo(Wallet.WalletStatus.FROZEN);
        }

        @Test
        @DisplayName("should unfreeze frozen wallet")
        void shouldUnfreezeFrozenWallet() {
            activeWallet.setStatus(Wallet.WalletStatus.FROZEN);
            WalletResponse mockResponse = mock(WalletResponse.class);
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);
            when(walletMapper.toResponse(any(Wallet.class))).thenReturn(mockResponse);

            WalletResponse response = walletService.unfreezeWallet(userId);

            assertThat(response).isNotNull();
            assertThat(activeWallet.getStatus()).isEqualTo(Wallet.WalletStatus.ACTIVE);
        }

        @Test
        @DisplayName("should throw when unfreezing non-frozen wallet")
        void shouldThrowWhenUnfreezingNonFrozenWallet() {
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            assertThatThrownBy(() -> walletService.unfreezeWallet(userId))
                    .isInstanceOf(WalletException.class)
                    .hasMessage("Wallet is not frozen");
        }
    }

    @Nested
    @DisplayName("Plan Upgrade")
    class PlanUpgradeTests {

        @Test
        @DisplayName("should upgrade wallet plan and update limits")
        void shouldUpgradePlanAndUpdateLimits() {
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

            walletService.upgradePlan(userId, "PRO");

            assertThat(activeWallet.getPlan()).isEqualTo(Wallet.AccountPlan.PRO);
            assertThat(activeWallet.getDailyTransactionLimit()).isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(activeWallet.getMonthlyTransactionLimit()).isEqualByComparingTo(new BigDecimal("100000.00"));
            assertThat(activeWallet.getMaxVirtualCards()).isEqualTo(10);
            assertThat(activeWallet.getMultiCurrencyEnabled()).isTrue();
            assertThat(activeWallet.getApiAccessEnabled()).isTrue();
        }

        @Test
        @DisplayName("should skip upgrade if already on same plan")
        void shouldSkipUpgradeIfSamePlan() {
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(activeWallet));

            walletService.upgradePlan(userId, "STARTER");

            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle null plan name gracefully")
        void shouldHandleNullPlanGracefully() {
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(activeWallet));

            walletService.upgradePlan(userId, null);

            // Should default to STARTER and skip since already STARTER
            verify(walletRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Release Reserved Funds")
    class ReleaseReservedFundsTests {

        @Test
        @DisplayName("should release reserved funds successfully")
        void shouldReleaseReservedFundsSuccessfully() {
            activeWallet.setReservedBalance(new BigDecimal("100.00"));
            BigDecimal amount = new BigDecimal("100.00");
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

            WalletOperationResponse response = walletService.releaseReservedFunds(userId, amount, "ref-1");

            assertThat(response.success()).isTrue();
            assertThat(response.operation()).isEqualTo("RELEASE_RESERVE");
        }
    }

    @Nested
    @DisplayName("Reverse Operations")
    class ReverseOperationTests {

        @Test
        @DisplayName("should reverse credit successfully")
        void shouldReverseCreditSuccessfully() {
            BigDecimal amount = new BigDecimal("100.00");
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

            WalletOperationResponse response = walletService.reverseCredit(userId, amount, "ref-1");

            assertThat(response.success()).isTrue();
            assertThat(response.operation()).isEqualTo("REVERSE_CREDIT");
        }

        @Test
        @DisplayName("should fail reverse credit when balance insufficient")
        void shouldFailReverseCreditWhenBalanceInsufficient() {
            activeWallet.setBalance(new BigDecimal("50.00"));
            BigDecimal amount = new BigDecimal("100.00");
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));

            WalletOperationResponse response = walletService.reverseCredit(userId, amount, "ref-1");

            assertThat(response.success()).isFalse();
        }

        @Test
        @DisplayName("should reverse deduction successfully")
        void shouldReverseDeductionSuccessfully() {
            BigDecimal amount = new BigDecimal("100.00");
            when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

            WalletOperationResponse response = walletService.reverseDeduction(userId, amount, "ref-1");

            assertThat(response.success()).isTrue();
            assertThat(response.operation()).isEqualTo("REVERSE_DEDUCTION");
        }
    }
}
