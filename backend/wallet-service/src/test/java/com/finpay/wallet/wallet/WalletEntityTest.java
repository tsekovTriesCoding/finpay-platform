package com.finpay.wallet.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Wallet Entity Unit Tests")
class WalletEntityTest {

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.builder()
                .balance(new BigDecimal("1000.00"))
                .reservedBalance(BigDecimal.ZERO)
                .spendTracker(new SpendTracker())
                .dailyTransactionLimit(new BigDecimal("500.00"))
                .monthlyTransactionLimit(new BigDecimal("5000.00"))
                .build();
    }

    @Nested
    @DisplayName("Available Balance")
    class AvailableBalanceTests {

        @Test
        @DisplayName("should return full balance when no reserved funds")
        void shouldReturnFullBalanceWhenNoReserved() {
            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("should subtract reserved from balance")
        void shouldSubtractReservedFromBalance() {
            wallet.setReservedBalance(new BigDecimal("200.00"));

            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        }
    }

    @Nested
    @DisplayName("Reserve Funds")
    class ReserveFundsTests {

        @Test
        @DisplayName("should reserve funds when sufficient balance")
        void shouldReserveFundsWhenSufficient() {
            boolean result = wallet.reserveFunds(new BigDecimal("300.00"));

            assertThat(result).isTrue();
            assertThat(wallet.getReservedBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        }

        @Test
        @DisplayName("should fail to reserve when insufficient balance")
        void shouldFailWhenInsufficientBalance() {
            boolean result = wallet.reserveFunds(new BigDecimal("1500.00"));

            assertThat(result).isFalse();
            assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should accumulate reserved amount")
        void shouldAccumulateReserved() {
            wallet.reserveFunds(new BigDecimal("200.00"));
            wallet.reserveFunds(new BigDecimal("300.00"));

            assertThat(wallet.getReservedBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("should track spend on reserve")
        void shouldTrackSpendOnReserve() {
            wallet.reserveFunds(new BigDecimal("200.00"));

            assertThat(wallet.getSpendTracker().getDailySpent()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(wallet.getSpendTracker().getMonthlySpent()).isEqualByComparingTo(new BigDecimal("200.00"));
        }
    }

    @Nested
    @DisplayName("Release Reserved Funds")
    class ReleaseReservedTests {

        @Test
        @DisplayName("should release reserved funds")
        void shouldReleaseReservedFunds() {
            wallet.reserveFunds(new BigDecimal("300.00"));

            wallet.releaseReservedFunds(new BigDecimal("300.00"));

            assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("should not go below zero reserved on over-release")
        void shouldNotGoBelowZeroOnOverRelease() {
            wallet.reserveFunds(new BigDecimal("100.00"));

            wallet.releaseReservedFunds(new BigDecimal("200.00"));

            assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should rollback spend tracker on release")
        void shouldRollbackSpendOnRelease() {
            wallet.reserveFunds(new BigDecimal("300.00"));
            wallet.releaseReservedFunds(new BigDecimal("200.00"));

            assertThat(wallet.getSpendTracker().getDailySpent()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(wallet.getSpendTracker().getMonthlySpent()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("Deduct Reserved Funds")
    class DeductReservedTests {

        @Test
        @DisplayName("should deduct from reserved and balance")
        void shouldDeductFromReservedAndBalance() {
            wallet.reserveFunds(new BigDecimal("300.00"));

            wallet.deductReservedFunds(new BigDecimal("300.00"));

            assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
            assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Credit Funds")
    class CreditFundsTests {

        @Test
        @DisplayName("should credit funds to balance")
        void shouldCreditFundsToBalance() {
            wallet.creditFunds(new BigDecimal("500.00"));

            assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }
    }
}
