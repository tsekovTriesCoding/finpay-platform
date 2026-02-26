package com.finpay.wallet.wallet;

import com.finpay.wallet.testconfig.TestMySQLContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMySQLContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("WalletRepository Data JPA Tests")
class WalletRepositoryTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID userId;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
        userId = UUID.randomUUID();
        testWallet = Wallet.builder()
                .userId(userId)
                .balance(new BigDecimal("1000.0000"))
                .reservedBalance(BigDecimal.ZERO)
                .currency("USD")
                .status(Wallet.WalletStatus.ACTIVE)
                .plan(Wallet.AccountPlan.STARTER)
                .dailyTransactionLimit(new BigDecimal("500.0000"))
                .monthlyTransactionLimit(new BigDecimal("5000.0000"))
                .spendTracker(new SpendTracker())
                .maxVirtualCards(1)
                .multiCurrencyEnabled(false)
                .apiAccessEnabled(false)
                .build();
    }

    @Nested
    @DisplayName("Save and Find")
    class SaveAndFindTests {

        @Test
        @DisplayName("should save and find wallet by ID")
        void shouldSaveAndFindById() {
            Wallet saved = walletRepository.save(testWallet);

            Optional<Wallet> found = walletRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(userId);
            assertThat(found.get().getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(found.get().getCurrency()).isEqualTo("USD");
            assertThat(found.get().getStatus()).isEqualTo(Wallet.WalletStatus.ACTIVE);
        }

        @Test
        @DisplayName("should auto-generate UUID on save")
        void shouldAutoGenerateUUID() {
            Wallet saved = walletRepository.save(testWallet);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("should set timestamps on save")
        void shouldSetTimestamps() {
            Wallet saved = walletRepository.save(testWallet);
            entityManager.flush();

            Wallet found = entityManager.find(Wallet.class, saved.getId());
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Find by User ID")
    class FindByUserIdTests {

        @Test
        @DisplayName("should find wallet by user ID")
        void shouldFindByUserId() {
            walletRepository.save(testWallet);

            Optional<Wallet> found = walletRepository.findByUserId(userId);

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("should return empty for non-existent user ID")
        void shouldReturnEmptyForNonExistentUser() {
            Optional<Wallet> found = walletRepository.findByUserId(UUID.randomUUID());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exists by User ID")
    class ExistsByUserIdTests {

        @Test
        @DisplayName("should return true when wallet exists")
        void shouldReturnTrueWhenExists() {
            walletRepository.save(testWallet);

            assertThat(walletRepository.existsByUserId(userId)).isTrue();
        }

        @Test
        @DisplayName("should return false when wallet does not exist")
        void shouldReturnFalseWhenNotExists() {
            assertThat(walletRepository.existsByUserId(UUID.randomUUID())).isFalse();
        }
    }

    @Nested
    @DisplayName("Pessimistic Locking")
    class PessimisticLockingTests {

        @Test
        @DisplayName("should find wallet for update by user ID")
        void shouldFindWalletForUpdateByUserId() {
            walletRepository.save(testWallet);

            Optional<Wallet> found = walletRepository.findByUserIdForUpdate(userId);

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("should find wallet for update by ID")
        void shouldFindWalletForUpdateById() {
            Wallet saved = walletRepository.save(testWallet);

            Optional<Wallet> found = walletRepository.findByIdForUpdate(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }
    }

    @Nested
    @DisplayName("Plan Persistence")
    class PlanPersistenceTests {

        @Test
        @DisplayName("should persist STARTER plan with correct limits")
        void shouldPersistStarterPlan() {
            Wallet saved = walletRepository.save(testWallet);

            Optional<Wallet> found = walletRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getPlan()).isEqualTo(Wallet.AccountPlan.STARTER);
            assertThat(found.get().getDailyTransactionLimit()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(found.get().getMonthlyTransactionLimit()).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("should persist PRO plan with correct limits")
        void shouldPersistProPlan() {
            testWallet.setPlan(Wallet.AccountPlan.PRO);
            testWallet.setDailyTransactionLimit(new BigDecimal("10000.0000"));
            testWallet.setMonthlyTransactionLimit(new BigDecimal("100000.0000"));
            testWallet.setMaxVirtualCards(10);
            testWallet.setMultiCurrencyEnabled(true);
            testWallet.setApiAccessEnabled(true);

            Wallet saved = walletRepository.save(testWallet);

            Optional<Wallet> found = walletRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getPlan()).isEqualTo(Wallet.AccountPlan.PRO);
            assertThat(found.get().getMaxVirtualCards()).isEqualTo(10);
            assertThat(found.get().getMultiCurrencyEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Balance Operations")
    class BalanceOperationTests {

        @Test
        @DisplayName("should persist balance changes")
        void shouldPersistBalanceChanges() {
            Wallet saved = walletRepository.save(testWallet);

            saved.setBalance(new BigDecimal("1500.0000"));
            walletRepository.save(saved);

            Optional<Wallet> found = walletRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("should persist reserved balance")
        void shouldPersistReservedBalance() {
            Wallet saved = walletRepository.save(testWallet);

            saved.setReservedBalance(new BigDecimal("200.0000"));
            walletRepository.save(saved);

            Optional<Wallet> found = walletRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getReservedBalance()).isEqualByComparingTo(new BigDecimal("200.00"));
        }
    }

    @Nested
    @DisplayName("SpendTracker Embedded")
    class SpendTrackerTests {

        @Test
        @DisplayName("should persist spend tracker data")
        void shouldPersistSpendTrackerData() {
            SpendTracker tracker = testWallet.getSpendTracker();
            tracker.setDailySpent(new BigDecimal("100.0000"));
            tracker.setMonthlySpent(new BigDecimal("500.0000"));

            Wallet saved = walletRepository.save(testWallet);

            Optional<Wallet> found = walletRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getSpendTracker().getDailySpent()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(found.get().getSpendTracker().getMonthlySpent()).isEqualByComparingTo(new BigDecimal("500.00"));
        }
    }
}
