package com.finpay.payment.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.billpayment.BillPayment;
import com.finpay.payment.billpayment.BillPaymentRepository;
import com.finpay.payment.request.MoneyRequest;
import com.finpay.payment.request.MoneyRequestRepository;
import com.finpay.payment.transfer.MoneyTransfer;
import com.finpay.payment.transfer.MoneyTransferRepository;
import com.finpay.payment.testconfig.TestcontainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@DisplayName("AdminTransactionController Integration Tests")
class AdminTransactionControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MoneyTransferRepository transferRepository;

    @Autowired
    private BillPaymentRepository billPaymentRepository;

    @Autowired
    private MoneyRequestRepository moneyRequestRepository;

    private static final AtomicInteger REF_SEQ = new AtomicInteger();

    @BeforeEach
    void setUp() {
        moneyRequestRepository.deleteAll();
        transferRepository.deleteAll();
        billPaymentRepository.deleteAll();
    }

    private String uniqueRef(String prefix) {
        return prefix + "-" + REF_SEQ.incrementAndGet();
    }

    private MoneyTransfer createTransfer(MoneyTransfer.TransferStatus status, BigDecimal amount) {
        MoneyTransfer transfer = MoneyTransfer.builder()
                .transactionReference(uniqueRef("TRF"))
                .senderUserId(UUID.randomUUID())
                .recipientUserId(UUID.randomUUID())
                .amount(amount)
                .currency("USD")
                .description("Test transfer")
                .status(status)
                .sagaStatus(MoneyTransfer.SagaStatus.COMPLETED)
                .build();
        return transferRepository.save(transfer);
    }

    private BillPayment createBillPayment(BillPayment.BillPaymentStatus status, BigDecimal amount) {
        BillPayment bill = BillPayment.builder()
                .transactionReference(uniqueRef("BILL"))
                .userId(UUID.randomUUID())
                .category(BillPayment.BillCategory.ELECTRICITY)
                .billerName("Power Co")
                .billerCode("PWR-001")
                .accountNumber("ACC-12345")
                .accountHolderName("John Doe")
                .amount(amount)
                .currency("USD")
                .status(status)
                .build();
        return billPaymentRepository.save(bill);
    }

    private MoneyRequest createMoneyRequest(MoneyRequest.RequestStatus status, BigDecimal amount) {
        MoneyRequest request = MoneyRequest.builder()
                .requestReference(uniqueRef("REQ"))
                .requesterUserId(UUID.randomUUID())
                .payerUserId(UUID.randomUUID())
                .amount(amount)
                .currency("USD")
                .description("Test request")
                .status(status)
                .sagaStatus(MoneyRequest.SagaStatus.NOT_STARTED)
                .build();
        return moneyRequestRepository.save(request);
    }

    @Nested
    @DisplayName("GET /api/v1/admin/transactions")
    class ListTransactions {

        @Test
        @DisplayName("should return transfers by default when no type specified")
        void shouldReturnTransfersByDefault() {
            createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("100.00"));
            createTransfer(MoneyTransfer.TransferStatus.PENDING, new BigDecimal("50.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions?page=0&size=10")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].type").isEqualTo("TRANSFER");
        }

        @Test
        @DisplayName("should return transfers when type=TRANSFER")
        void shouldReturnTransfers() {
            createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("200.00"));
            createBillPayment(BillPayment.BillPaymentStatus.COMPLETED, new BigDecimal("75.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions?type=TRANSFER")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].type").isEqualTo("TRANSFER");
        }

        @Test
        @DisplayName("should return bill payments when type=BILL_PAYMENT")
        void shouldReturnBillPayments() {
            createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("200.00"));
            createBillPayment(BillPayment.BillPaymentStatus.COMPLETED, new BigDecimal("75.00"));
            createBillPayment(BillPayment.BillPaymentStatus.FAILED, new BigDecimal("30.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions?type=BILL_PAYMENT")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].type").isEqualTo("BILL_PAYMENT");
        }

        @Test
        @DisplayName("should return money requests when type=MONEY_REQUEST")
        void shouldReturnMoneyRequests() {
            createMoneyRequest(MoneyRequest.RequestStatus.PENDING_APPROVAL, new BigDecimal("25.00"));
            createMoneyRequest(MoneyRequest.RequestStatus.COMPLETED, new BigDecimal("50.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions?type=MONEY_REQUEST")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].type").isEqualTo("MONEY_REQUEST");
        }

        @Test
        @DisplayName("should filter transfers by status")
        void shouldFilterTransfersByStatus() {
            createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("100.00"));
            createTransfer(MoneyTransfer.TransferStatus.FAILED, new BigDecimal("50.00"));
            createTransfer(MoneyTransfer.TransferStatus.PENDING, new BigDecimal("75.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions?type=TRANSFER&status=COMPLETED")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].status").isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("should filter bill payments by status")
        void shouldFilterBillPaymentsByStatus() {
            createBillPayment(BillPayment.BillPaymentStatus.COMPLETED, new BigDecimal("100.00"));
            createBillPayment(BillPayment.BillPaymentStatus.FAILED, new BigDecimal("50.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions?type=BILL_PAYMENT&status=FAILED")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].status").isEqualTo("FAILED");
        }

        @Test
        @DisplayName("should filter money requests by status")
        void shouldFilterMoneyRequestsByStatus() {
            createMoneyRequest(MoneyRequest.RequestStatus.PENDING_APPROVAL, new BigDecimal("25.00"));
            createMoneyRequest(MoneyRequest.RequestStatus.DECLINED, new BigDecimal("50.00"));
            createMoneyRequest(MoneyRequest.RequestStatus.COMPLETED, new BigDecimal("100.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions?type=MONEY_REQUEST&status=DECLINED")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].status").isEqualTo("DECLINED");
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void shouldRespectPagination() {
            for (int i = 0; i < 5; i++) {
                createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("10.00"));
            }

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions?page=0&size=2")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.page.totalElements").isEqualTo(5);
            assertThat(result).bodyJson()
                    .extractingPath("$.page.totalPages").isEqualTo(3);
        }

        @Test
        @DisplayName("should return empty page when no transactions exist")
        void shouldReturnEmptyPage() {
            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("should contain correct fields in transaction response")
        void shouldContainCorrectFields() {
            MoneyTransfer transfer = createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("250.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions?type=TRANSFER")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].id").isEqualTo(transfer.getId().toString());
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].transactionReference").isEqualTo(transfer.getTransactionReference());
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].amount").isNotNull();
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].currency").isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/transactions/metrics")
    class TransactionMetrics {

        @Test
        @DisplayName("should return correct metrics with mixed transaction data")
        void shouldReturnCorrectMetrics() {
            createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("100.00"));
            createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("200.00"));
            createTransfer(MoneyTransfer.TransferStatus.FAILED, new BigDecimal("50.00"));
            createTransfer(MoneyTransfer.TransferStatus.PENDING, new BigDecimal("75.00"));

            createBillPayment(BillPayment.BillPaymentStatus.COMPLETED, new BigDecimal("80.00"));
            createBillPayment(BillPayment.BillPaymentStatus.FAILED, new BigDecimal("30.00"));

            createMoneyRequest(MoneyRequest.RequestStatus.PENDING_APPROVAL, new BigDecimal("40.00"));
            createMoneyRequest(MoneyRequest.RequestStatus.COMPLETED, new BigDecimal("60.00"));
            createMoneyRequest(MoneyRequest.RequestStatus.DECLINED, new BigDecimal("20.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions/metrics")
                    .exchange();

            assertThat(result).hasStatusOk();

            assertThat(result).bodyJson()
                    .extractingPath("$.totalTransfers").isEqualTo(4);
            assertThat(result).bodyJson()
                    .extractingPath("$.completedTransfers").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.failedTransfers").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.pendingTransfers").isEqualTo(1);

            assertThat(result).bodyJson()
                    .extractingPath("$.totalBillPayments").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.completedBillPayments").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.failedBillPayments").isEqualTo(1);

            assertThat(result).bodyJson()
                    .extractingPath("$.totalMoneyRequests").isEqualTo(3);
            assertThat(result).bodyJson()
                    .extractingPath("$.pendingMoneyRequests").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.completedMoneyRequests").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.declinedMoneyRequests").isEqualTo(1);
        }

        @Test
        @DisplayName("should return zero metrics when no transactions exist")
        void shouldReturnZeroMetrics() {
            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions/metrics")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalTransfers").isEqualTo(0);
            assertThat(result).bodyJson()
                    .extractingPath("$.totalBillPayments").isEqualTo(0);
            assertThat(result).bodyJson()
                    .extractingPath("$.totalMoneyRequests").isEqualTo(0);
        }

        @Test
        @DisplayName("should calculate correct transfer volume for completed transfers only")
        void shouldCalculateTransferVolume() {
            createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("150.00"));
            createTransfer(MoneyTransfer.TransferStatus.COMPLETED, new BigDecimal("250.00"));
            createTransfer(MoneyTransfer.TransferStatus.FAILED, new BigDecimal("999.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions/metrics")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalTransferVolume").satisfies(vol -> {
                        BigDecimal volume = new BigDecimal(vol.toString());
                        assertThat(volume).isEqualByComparingTo("400.00");
                    });
        }

        @Test
        @DisplayName("should calculate correct bill payment volume for completed bills only")
        void shouldCalculateBillPaymentVolume() {
            createBillPayment(BillPayment.BillPaymentStatus.COMPLETED, new BigDecimal("120.00"));
            createBillPayment(BillPayment.BillPaymentStatus.COMPLETED, new BigDecimal("80.00"));
            createBillPayment(BillPayment.BillPaymentStatus.FAILED, new BigDecimal("500.00"));

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/transactions/metrics")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalBillPaymentVolume").satisfies(vol -> {
                        BigDecimal volume = new BigDecimal(vol.toString());
                        assertThat(volume).isEqualByComparingTo("200.00");
                    });
        }
    }
}
