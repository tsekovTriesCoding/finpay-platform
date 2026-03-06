package com.finpay.wallet.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.wallet.Wallet;
import com.finpay.wallet.wallet.WalletRepository;
import com.finpay.wallet.transaction.WalletTransactionRepository;
import com.finpay.wallet.testconfig.TestcontainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@DisplayName("AdminWalletController Integration Tests")
class AdminWalletControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private static final UUID ADMIN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        walletTransactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    private Wallet createWalletInDb(UUID userId, BigDecimal balance, Wallet.WalletStatus status) {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(balance)
                .reservedBalance(BigDecimal.ZERO)
                .currency("USD")
                .status(status)
                .plan(Wallet.AccountPlan.STARTER)
                .dailyTransactionLimit(new BigDecimal("500.00"))
                .monthlyTransactionLimit(new BigDecimal("5000.00"))
                .maxVirtualCards(1)
                .multiCurrencyEnabled(false)
                .apiAccessEnabled(false)
                .build();
        return walletRepository.save(wallet);
    }

    private Wallet createActiveWallet(UUID userId, BigDecimal balance) {
        return createWalletInDb(userId, balance, Wallet.WalletStatus.ACTIVE);
    }

    @Nested
    @DisplayName("GET /api/v1/admin/wallets")
    class ListWallets {

        @Test
        @DisplayName("should return paginated list of all wallets")
        void shouldReturnAllWallets() {
            createActiveWallet(UUID.randomUUID(), new BigDecimal("100.00"));
            createActiveWallet(UUID.randomUUID(), new BigDecimal("200.00"));
            createActiveWallet(UUID.randomUUID(), new BigDecimal("300.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/wallets?page=0&size=10")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(3);
            assertThat(result).bodyJson()
                    .extractingPath("$.totalElements").isEqualTo(3);
        }

        @Test
        @DisplayName("should filter wallets by ACTIVE status")
        void shouldFilterByActiveStatus() {
            createActiveWallet(UUID.randomUUID(), new BigDecimal("100.00"));
            createWalletInDb(UUID.randomUUID(), new BigDecimal("200.00"), Wallet.WalletStatus.FROZEN);

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/wallets?status=ACTIVE")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].status").isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should filter wallets by FROZEN status")
        void shouldFilterByFrozenStatus() {
            createActiveWallet(UUID.randomUUID(), new BigDecimal("100.00"));
            createWalletInDb(UUID.randomUUID(), new BigDecimal("200.00"), Wallet.WalletStatus.FROZEN);
            createWalletInDb(UUID.randomUUID(), new BigDecimal("300.00"), Wallet.WalletStatus.FROZEN);

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/wallets?status=FROZEN")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(2);
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void shouldRespectPagination() {
            for (int i = 0; i < 5; i++) {
                createActiveWallet(UUID.randomUUID(), new BigDecimal("100.00"));
            }

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/wallets?page=0&size=2")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.totalElements").isEqualTo(5);
            assertThat(result).bodyJson()
                    .extractingPath("$.totalPages").isEqualTo(3);
        }

        @Test
        @DisplayName("should return empty page when no wallets exist")
        void shouldReturnEmptyPage() {
            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/wallets")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("should return wallet response with all expected fields")
        void shouldContainExpectedFields() {
            UUID userId = UUID.randomUUID();
            Wallet wallet = createActiveWallet(userId, new BigDecimal("500.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/wallets")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].userId").isEqualTo(userId.toString());
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].currency").isEqualTo("USD");
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].status").isEqualTo("ACTIVE");
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].plan").isEqualTo("STARTER");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/wallets/user/{userId}/freeze")
    class FreezeWallet {

        @Test
        @DisplayName("should freeze an active wallet")
        void shouldFreezeWallet() {
            UUID userId = UUID.randomUUID();
            createActiveWallet(userId, new BigDecimal("500.00"));

            MvcTestResult result = mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/freeze", userId)
                    .header("X-User-Id", ADMIN_ID.toString())
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("FROZEN");

            Wallet updated = walletRepository.findByUserId(userId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(Wallet.WalletStatus.FROZEN);
        }

        @Test
        @DisplayName("should return 404 for non-existent wallet")
        void shouldReturn404ForNonExistentWallet() {
            assertThat(mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/freeze", UUID.randomUUID())
                    .header("X-User-Id", ADMIN_ID.toString()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should preserve wallet balance after freeze")
        void shouldPreserveBalanceAfterFreeze() {
            UUID userId = UUID.randomUUID();
            createActiveWallet(userId, new BigDecimal("1234.56"));

            mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/freeze", userId)
                    .header("X-User-Id", ADMIN_ID.toString())
                    .exchange();

            Wallet frozen = walletRepository.findByUserId(userId).orElseThrow();
            assertThat(frozen.getBalance()).isEqualByComparingTo("1234.5600");
            assertThat(frozen.getStatus()).isEqualTo(Wallet.WalletStatus.FROZEN);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/wallets/user/{userId}/unfreeze")
    class UnfreezeWallet {

        @Test
        @DisplayName("should unfreeze a frozen wallet")
        void shouldUnfreezeWallet() {
            UUID userId = UUID.randomUUID();
            createWalletInDb(userId, new BigDecimal("500.00"), Wallet.WalletStatus.FROZEN);

            MvcTestResult result = mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/unfreeze", userId)
                    .header("X-User-Id", ADMIN_ID.toString())
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("ACTIVE");

            Wallet updated = walletRepository.findByUserId(userId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(Wallet.WalletStatus.ACTIVE);
        }

        @Test
        @DisplayName("should return 400 when unfreezing an active wallet")
        void shouldRejectUnfreezeOnActiveWallet() {
            UUID userId = UUID.randomUUID();
            createActiveWallet(userId, new BigDecimal("500.00"));

            assertThat(mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/unfreeze", userId)
                    .header("X-User-Id", ADMIN_ID.toString()))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 404 for non-existent wallet")
        void shouldReturn404ForNonExistentWallet() {
            assertThat(mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/unfreeze", UUID.randomUUID())
                    .header("X-User-Id", ADMIN_ID.toString()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/wallets/metrics")
    class WalletMetrics {

        @Test
        @DisplayName("should return correct KPI metrics")
        void shouldReturnCorrectMetrics() {
            createActiveWallet(UUID.randomUUID(), new BigDecimal("100.00"));
            createActiveWallet(UUID.randomUUID(), new BigDecimal("200.00"));
            createWalletInDb(UUID.randomUUID(), new BigDecimal("300.00"), Wallet.WalletStatus.FROZEN);
            createWalletInDb(UUID.randomUUID(), new BigDecimal("50.00"), Wallet.WalletStatus.CLOSED);

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/wallets/metrics")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalWallets").isEqualTo(4);
            assertThat(result).bodyJson()
                    .extractingPath("$.activeWallets").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.frozenWallets").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.closedWallets").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.totalBalance").satisfies(bal -> {
                        BigDecimal totalBalance = new BigDecimal(bal.toString());
                        assertThat(totalBalance).isEqualByComparingTo("650.00");
                    });
        }

        @Test
        @DisplayName("should return zero metrics when no wallets exist")
        void shouldReturnZeroMetrics() {
            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/wallets/metrics")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalWallets").isEqualTo(0);
            assertThat(result).bodyJson()
                    .extractingPath("$.activeWallets").isEqualTo(0);
            assertThat(result).bodyJson()
                    .extractingPath("$.frozenWallets").isEqualTo(0);
            assertThat(result).bodyJson()
                    .extractingPath("$.closedWallets").isEqualTo(0);
        }

        @Test
        @DisplayName("should update metrics after freezing a wallet")
        void shouldUpdateMetricsAfterFreeze() {
            UUID userId = UUID.randomUUID();
            createActiveWallet(userId, new BigDecimal("500.00"));
            createActiveWallet(UUID.randomUUID(), new BigDecimal("300.00"));

            mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/freeze", userId)
                    .header("X-User-Id", ADMIN_ID.toString())
                    .exchange();

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/wallets/metrics")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalWallets").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.activeWallets").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.frozenWallets").isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("End-to-End Admin Workflows")
    class EndToEndWorkflows {

        @Test
        @DisplayName("should freeze and unfreeze a wallet with full lifecycle")
        void shouldFreezeAndUnfreezeLifecycle() {
            UUID userId = UUID.randomUUID();
            createActiveWallet(userId, new BigDecimal("1000.00"));

            MvcTestResult freezeResult = mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/freeze", userId)
                    .header("X-User-Id", ADMIN_ID.toString())
                    .exchange();

            assertThat(freezeResult).hasStatusOk();
            assertThat(freezeResult).bodyJson()
                    .extractingPath("$.status").isEqualTo("FROZEN");

            MvcTestResult unfreezeResult = mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/unfreeze", userId)
                    .header("X-User-Id", ADMIN_ID.toString())
                    .exchange();

            assertThat(unfreezeResult).hasStatusOk();
            assertThat(unfreezeResult).bodyJson()
                    .extractingPath("$.status").isEqualTo("ACTIVE");

            Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
            assertThat(wallet.getStatus()).isEqualTo(Wallet.WalletStatus.ACTIVE);
            assertThat(wallet.getBalance()).isEqualByComparingTo("1000.0000");
        }

        @Test
        @DisplayName("should reflect wallet status changes in list and metrics")
        void shouldReflectChangesInListAndMetrics() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            createActiveWallet(userId1, new BigDecimal("100.00"));
            createActiveWallet(userId2, new BigDecimal("200.00"));

            MvcTestResult listBefore = mvc.get()
                    .uri("/api/v1/admin/wallets?status=ACTIVE")
                    .exchange();
            assertThat(listBefore).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(2);

            mvc.post()
                    .uri("/api/v1/admin/wallets/user/{userId}/freeze", userId1)
                    .header("X-User-Id", ADMIN_ID.toString())
                    .exchange();

            MvcTestResult listAfter = mvc.get()
                    .uri("/api/v1/admin/wallets?status=ACTIVE")
                    .exchange();
            assertThat(listAfter).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);

            MvcTestResult frozenList = mvc.get()
                    .uri("/api/v1/admin/wallets?status=FROZEN")
                    .exchange();
            assertThat(frozenList).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
        }
    }
}
