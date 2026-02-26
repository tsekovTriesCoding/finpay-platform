package com.finpay.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.testconfig.TestcontainersConfig;
import com.finpay.wallet.wallet.Wallet;
import com.finpay.wallet.wallet.WalletRepository;
import com.finpay.wallet.wallet.dto.WalletOperationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@DisplayName("WalletController Integration Tests")
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
    }

    private Wallet createWalletInDb(UUID userId, BigDecimal balance) {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(balance)
                .reservedBalance(BigDecimal.ZERO)
                .currency("USD")
                .status(Wallet.WalletStatus.ACTIVE)
                .plan(Wallet.AccountPlan.STARTER)
                .dailyTransactionLimit(new BigDecimal("500.00"))
                .monthlyTransactionLimit(new BigDecimal("5000.00"))
                .maxVirtualCards(1)
                .multiCurrencyEnabled(false)
                .apiAccessEnabled(false)
                .build();
        return walletRepository.save(wallet);
    }

    @Nested
    @DisplayName("GET /api/v1/wallets/user/{userId}")
    class GetWallet {

        @Test
        @DisplayName("should return 404 when wallet does not exist")
        void shouldReturn404WhenWalletNotFound() {
            assertThat(mvc.get().uri("/api/v1/wallets/user/{userId}", UUID.randomUUID()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should return existing wallet with all fields")
        void shouldReturnExistingWallet() {
            Wallet wallet = createWalletInDb(TEST_USER_ID, new BigDecimal("500.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/wallets/user/{userId}", TEST_USER_ID).exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.id").isEqualTo(wallet.getId().toString());
            assertThat(result).bodyJson()
                    .extractingPath("$.userId").isEqualTo(TEST_USER_ID.toString());
            assertThat(result).bodyJson()
                    .extractingPath("$.currency").isEqualTo("USD");
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("ACTIVE");
            assertThat(result).bodyJson()
                    .extractingPath("$.plan").isEqualTo("STARTER");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/wallets/deposit")
    class Deposit {

        @Test
        @DisplayName("should deposit funds successfully")
        void shouldDepositFunds() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("100.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("50.00"), "DEP-001", "Test deposit");

            MvcTestResult result = mvc.post().uri("/api/v1/wallets/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
            assertThat(result).bodyJson()
                    .extractingPath("$.operation").isEqualTo("DEPOSIT");

            Wallet updated = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(updated.getBalance()).isEqualByComparingTo("150.0000");
        }

        @Test
        @DisplayName("should return 400 when depositing to frozen wallet")
        void shouldRejectDepositToFrozenWallet() throws Exception {
            Wallet wallet = createWalletInDb(TEST_USER_ID, new BigDecimal("100.00"));
            wallet.setStatus(Wallet.WalletStatus.FROZEN);
            walletRepository.save(wallet);

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("50.00"), "DEP-F01", "Deposit to frozen");

            assertThat(mvc.post().uri("/api/v1/wallets/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 400 for invalid amount (zero)")
        void shouldRejectZeroAmount() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("100.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("0.00"), "DEP-Z01", "Zero deposit");

            assertThat(mvc.post().uri("/api/v1/wallets/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 400 for null userId")
        void shouldRejectNullUserId() throws Exception {
            WalletOperationRequest request = new WalletOperationRequest(
                    null, new BigDecimal("50.00"), "DEP-N01", "Null user");

            assertThat(mvc.post().uri("/api/v1/wallets/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/wallets/withdraw")
    class Withdraw {

        @Test
        @DisplayName("should withdraw funds successfully")
        void shouldWithdrawFunds() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("200.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("50.00"), "WD-001", "Test withdraw");

            MvcTestResult result = mvc.post().uri("/api/v1/wallets/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
            assertThat(result).bodyJson()
                    .extractingPath("$.operation").isEqualTo("WITHDRAWAL");

            Wallet updated = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(updated.getBalance()).isEqualByComparingTo("150.0000");
        }

        @Test
        @DisplayName("should return 400 for insufficient funds")
        void shouldRejectInsufficientFunds() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("10.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("100.00"), "WD-002", "Too much");

            assertThat(mvc.post().uri("/api/v1/wallets/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 400 when withdrawing from frozen wallet")
        void shouldRejectWithdrawFromFrozenWallet() throws Exception {
            Wallet wallet = createWalletInDb(TEST_USER_ID, new BigDecimal("500.00"));
            wallet.setStatus(Wallet.WalletStatus.FROZEN);
            walletRepository.save(wallet);

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("50.00"), "WD-F01", "Frozen withdraw");

            assertThat(mvc.post().uri("/api/v1/wallets/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Reserve / Release / Deduct / Credit")
    class FundsOperations {

        @Test
        @DisplayName("should reserve funds and track reserved balance")
        void shouldReserveFunds() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("500.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("100.00"), "RES-001", "Reserve for transfer");

            MvcTestResult result = mvc.post().uri("/api/v1/wallets/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            Wallet updated = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(updated.getReservedBalance()).isEqualByComparingTo("100.0000");
            assertThat(updated.getBalance()).isEqualByComparingTo("500.0000");
        }

        @Test
        @DisplayName("should return 400 when reserving more than available balance")
        void shouldRejectReserveExceedingBalance() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("100.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("200.00"), "RES-002", "Too much");

            assertThat(mvc.post().uri("/api/v1/wallets/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should release reserved funds")
        void shouldReleaseReservedFunds() throws Exception {
            Wallet wallet = createWalletInDb(TEST_USER_ID, new BigDecimal("500.00"));
            wallet.setReservedBalance(new BigDecimal("100.00"));
            walletRepository.save(wallet);

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("100.00"), "REL-001", "Release reserve");

            assertThat(mvc.post().uri("/api/v1/wallets/release")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            Wallet updated = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(updated.getReservedBalance()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should deduct reserved funds and reduce balance")
        void shouldDeductReservedFunds() throws Exception {
            Wallet wallet = createWalletInDb(TEST_USER_ID, new BigDecimal("500.00"));
            wallet.setReservedBalance(new BigDecimal("100.00"));
            walletRepository.save(wallet);

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("100.00"), "DED-001", "Deduct for transfer");

            assertThat(mvc.post().uri("/api/v1/wallets/deduct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            Wallet updated = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(updated.getBalance()).isEqualByComparingTo("400.0000");
            assertThat(updated.getReservedBalance()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should return 400 when deducting more than reserved")
        void shouldRejectDeductExceedingReserved() throws Exception {
            Wallet wallet = createWalletInDb(TEST_USER_ID, new BigDecimal("500.00"));
            wallet.setReservedBalance(new BigDecimal("50.00"));
            walletRepository.save(wallet);

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("100.00"), "DED-002", "Too much");

            assertThat(mvc.post().uri("/api/v1/wallets/deduct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should credit funds")
        void shouldCreditFunds() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("200.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("100.00"), "CRD-001", "Credit from transfer");

            assertThat(mvc.post().uri("/api/v1/wallets/credit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            Wallet updated = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(updated.getBalance()).isEqualByComparingTo("300.0000");
        }

        @Test
        @DisplayName("should return 400 when crediting frozen wallet")
        void shouldRejectCreditToFrozenWallet() throws Exception {
            Wallet wallet = createWalletInDb(TEST_USER_ID, new BigDecimal("200.00"));
            wallet.setStatus(Wallet.WalletStatus.FROZEN);
            walletRepository.save(wallet);

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("100.00"), "CRD-F01", "Credit frozen");

            assertThat(mvc.post().uri("/api/v1/wallets/credit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Reverse Operations")
    class ReverseOperations {

        @Test
        @DisplayName("should reverse a credit (compensation)")
        void shouldReverseCredit() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("300.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("100.00"), "REV-C01", "Reverse credit");

            assertThat(mvc.post().uri("/api/v1/wallets/reverse-credit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            Wallet updated = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(updated.getBalance()).isEqualByComparingTo("200.0000");
        }

        @Test
        @DisplayName("should reverse a deduction (compensation)")
        void shouldReverseDeduction() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("200.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("50.00"), "REV-D01", "Reverse deduction");

            assertThat(mvc.post().uri("/api/v1/wallets/reverse-deduction")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            Wallet updated = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(updated.getBalance()).isEqualByComparingTo("250.0000");
        }
    }

    @Nested
    @DisplayName("Freeze / Unfreeze")
    class FreezeUnfreeze {

        @Test
        @DisplayName("should freeze wallet")
        void shouldFreezeWallet() {
            createWalletInDb(TEST_USER_ID, new BigDecimal("100.00"));

            assertThat(mvc.post().uri("/api/v1/wallets/user/{userId}/freeze", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.status").isEqualTo("FROZEN");
        }

        @Test
        @DisplayName("should unfreeze a frozen wallet")
        void shouldUnfreezeWallet() {
            Wallet wallet = createWalletInDb(TEST_USER_ID, new BigDecimal("100.00"));
            wallet.setStatus(Wallet.WalletStatus.FROZEN);
            walletRepository.save(wallet);

            assertThat(mvc.post().uri("/api/v1/wallets/user/{userId}/unfreeze", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.status").isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should return 400 when unfreezing an active wallet")
        void shouldRejectUnfreezeActiveWallet() {
            createWalletInDb(TEST_USER_ID, new BigDecimal("100.00"));

            assertThat(mvc.post().uri("/api/v1/wallets/user/{userId}/unfreeze", TEST_USER_ID))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/wallets/user/{userId}/transactions")
    class Transactions {

        @Test
        @DisplayName("should return transaction history after deposit")
        void shouldReturnTransactions() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("500.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("100.00"), "TXN-001", "Test txn");
            mvc.post().uri("/api/v1/wallets/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(mvc.get().uri("/api/v1/wallets/user/{userId}/transactions", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content").isNotNull();
        }

        @Test
        @DisplayName("should return recent transactions")
        void shouldReturnRecentTransactions() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("500.00"));

            WalletOperationRequest request = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("25.00"), "TXN-REC", "Recent txn");
            mvc.post().uri("/api/v1/wallets/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(mvc.get().uri("/api/v1/wallets/user/{userId}/transactions/recent", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.length()").satisfies(len ->
                            assertThat((Integer) len).isGreaterThanOrEqualTo(1));
        }
    }

    @Nested
    @DisplayName("Full Reserve-Deduct Lifecycle")
    class FullLifecycle {

        @Test
        @DisplayName("should complete reserve → deduct lifecycle")
        void shouldCompleteReserveDeductLifecycle() throws Exception {
            createWalletInDb(TEST_USER_ID, new BigDecimal("500.00"));

            // Step 1: Reserve
            WalletOperationRequest reserveReq = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("200.00"), "LFC-001", "Reserve");
            mvc.post().uri("/api/v1/wallets/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reserveReq))
                    .exchange();

            Wallet afterReserve = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(afterReserve.getBalance()).isEqualByComparingTo("500.0000");
            assertThat(afterReserve.getReservedBalance()).isEqualByComparingTo("200.0000");

            // Step 2: Deduct reserved
            WalletOperationRequest deductReq = new WalletOperationRequest(
                    TEST_USER_ID, new BigDecimal("200.00"), "LFC-001", "Deduct");
            mvc.post().uri("/api/v1/wallets/deduct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deductReq))
                    .exchange();

            Wallet afterDeduct = walletRepository.findByUserId(TEST_USER_ID).orElseThrow();
            assertThat(afterDeduct.getBalance()).isEqualByComparingTo("300.0000");
            assertThat(afterDeduct.getReservedBalance()).isEqualByComparingTo("0");
        }
    }
}
