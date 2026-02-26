package com.finpay.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.billpayment.BillPayment;
import com.finpay.payment.billpayment.BillPaymentRepository;
import com.finpay.payment.billpayment.dto.BillPaymentRequest;
import com.finpay.payment.payment.Payment;
import com.finpay.payment.payment.PaymentRepository;
import com.finpay.payment.payment.dto.PaymentRequest;
import com.finpay.payment.request.MoneyRequestRepository;
import com.finpay.payment.request.dto.MoneyRequestCreateDto;
import com.finpay.payment.transfer.MoneyTransferRepository;
import com.finpay.payment.transfer.dto.MoneyTransferRequest;
import com.finpay.payment.testconfig.TestcontainersConfig;
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
@DisplayName("PaymentController Integration Tests")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BillPaymentRepository billPaymentRepository;

    @Autowired
    private MoneyTransferRepository moneyTransferRepository;

    @Autowired
    private MoneyRequestRepository moneyRequestRepository;

    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        moneyRequestRepository.deleteAll();
        moneyTransferRepository.deleteAll();
        billPaymentRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    private PaymentRequest validPaymentRequest() {
        return new PaymentRequest(
                TEST_USER_ID,
                new BigDecimal("100.00"),
                "USD",
                Payment.PaymentMethod.WALLET,
                Payment.PaymentType.TRANSFER,
                "Test payment",
                null, null, null, null,
                null, null, null, null, null
        );
    }

    private String createPaymentAndReturnId() throws Exception {
        MvcTestResult result = mvc.post().uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPaymentRequest()))
                .exchange();
        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("id").asText();
    }

    @Nested
    @DisplayName("POST /api/v1/payments")
    class CreatePayment {

        @Test
        @DisplayName("should create a payment with generated reference and processing fee")
        void shouldCreatePayment() throws Exception {
            MvcTestResult result = mvc.post().uri("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPaymentRequest()))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson()
                    .extractingPath("$.userId").isEqualTo(TEST_USER_ID.toString());
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("PENDING");
            assertThat(result).bodyJson()
                    .extractingPath("$.transactionReference").asString().startsWith("FP");
            assertThat(result).bodyJson()
                    .extractingPath("$.processingFee").isNotNull();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalAmount").isNotNull();
        }

        @Test
        @DisplayName("should return 400 for null required fields")
        void shouldRejectInvalidPayment() throws Exception {
            PaymentRequest invalid = new PaymentRequest(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null
            );

            assertThat(mvc.post().uri("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 400 for zero amount")
        void shouldRejectZeroAmount() throws Exception {
            PaymentRequest request = new PaymentRequest(
                    TEST_USER_ID, new BigDecimal("0.00"), "USD",
                    Payment.PaymentMethod.WALLET, Payment.PaymentType.TRANSFER,
                    "Zero payment", null, null, null, null,
                    null, null, null, null, null
            );

            assertThat(mvc.post().uri("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 400 for blank currency")
        void shouldRejectBlankCurrency() throws Exception {
            PaymentRequest request = new PaymentRequest(
                    TEST_USER_ID, new BigDecimal("50.00"), "",
                    Payment.PaymentMethod.WALLET, Payment.PaymentType.TRANSFER,
                    "No currency", null, null, null, null,
                    null, null, null, null, null
            );

            assertThat(mvc.post().uri("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{id}")
    class GetPaymentById {

        @Test
        @DisplayName("should return payment by ID")
        void shouldReturnPaymentById() throws Exception {
            String paymentId = createPaymentAndReturnId();

            MvcTestResult result = mvc.get().uri("/api/v1/payments/{id}", paymentId).exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.description").isEqualTo("Test payment");
            assertThat(result).bodyJson()
                    .extractingPath("$.paymentMethod").isEqualTo("WALLET");
        }

        @Test
        @DisplayName("should return 404 for unknown payment ID")
        void shouldReturn404ForUnknownId() {
            assertThat(mvc.get().uri("/api/v1/payments/{id}", UUID.randomUUID()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/reference/{ref}")
    class GetPaymentByReference {

        @Test
        @DisplayName("should find payment by transaction reference")
        void shouldFindByReference() throws Exception {
            MvcTestResult createResult = mvc.post().uri("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPaymentRequest()))
                    .exchange();

            String ref = objectMapper.readTree(
                    createResult.getResponse().getContentAsString()
            ).get("transactionReference").asText();

            assertThat(mvc.get().uri("/api/v1/payments/reference/{ref}", ref))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.transactionReference").isEqualTo(ref);
        }

        @Test
        @DisplayName("should return 404 for unknown reference")
        void shouldReturn404ForUnknownRef() {
            assertThat(mvc.get().uri("/api/v1/payments/reference/{ref}", "NONEXISTENT"))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/user/{userId}")
    class GetPaymentsByUser {

        @Test
        @DisplayName("should return payments for user")
        void shouldReturnPaymentsForUser() throws Exception {
            createPaymentAndReturnId();

            assertThat(mvc.get().uri("/api/v1/payments/user/{userId}", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.length()").isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty list for user with no payments")
        void shouldReturnEmptyForNewUser() {
            assertThat(mvc.get().uri("/api/v1/payments/user/{userId}", UUID.randomUUID()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("should return paginated payments")
        void shouldReturnPaginatedPayments() throws Exception {
            createPaymentAndReturnId();
            createPaymentAndReturnId();

            assertThat(mvc.get().uri("/api/v1/payments/user/{userId}/paged?page=0&size=1", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/cancel")
    class CancelPayment {

        @Test
        @DisplayName("should cancel a pending payment")
        void shouldCancelPendingPayment() throws Exception {
            String paymentId = createPaymentAndReturnId();

            // Cancel quickly before async processing finishes
            assertThat(mvc.post().uri("/api/v1/payments/{id}/cancel", paymentId))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.status").isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should return 404 for unknown payment")
        void shouldReturn404ForUnknownPayment() {
            assertThat(mvc.post().uri("/api/v1/payments/{id}/cancel", UUID.randomUUID()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Bill Payments")
    class BillPayments {

        @Test
        @DisplayName("should create a bill payment")
        void shouldCreateBillPayment() throws Exception {
            BillPaymentRequest request = new BillPaymentRequest(
                    TEST_USER_ID,
                    BillPayment.BillCategory.ELECTRICITY,
                    "Power Co",
                    "PWR-001",
                    "ACC-12345",
                    "John Doe",
                    new BigDecimal("75.00"),
                    "USD",
                    "Monthly electricity bill"
            );

            MvcTestResult result = mvc.post().uri("/api/v1/payments/bills")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson()
                    .extractingPath("$.category").isEqualTo("ELECTRICITY");
            assertThat(result).bodyJson()
                    .extractingPath("$.billerName").isEqualTo("Power Co");
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("PENDING");
        }

        @Test
        @DisplayName("should return all bill categories")
        void shouldReturnCategories() {
            assertThat(mvc.get().uri("/api/v1/payments/bills/categories"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.length()").satisfies(len ->
                            assertThat((Integer) len).isGreaterThan(0));
        }

        @Test
        @DisplayName("should get bill payment by ID")
        void shouldGetBillPaymentById() throws Exception {
            BillPaymentRequest request = new BillPaymentRequest(
                    TEST_USER_ID,
                    BillPayment.BillCategory.WATER,
                    "Water Corp",
                    "WTR-001",
                    "ACC-67890",
                    "John Doe",
                    new BigDecimal("45.00"),
                    "USD",
                    "Water bill"
            );

            MvcTestResult createResult = mvc.post().uri("/api/v1/payments/bills")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            String billId = objectMapper.readTree(
                    createResult.getResponse().getContentAsString()
            ).get("id").asText();

            assertThat(mvc.get().uri("/api/v1/payments/bills/{id}", billId))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.billerName").isEqualTo("Water Corp");
        }

        @Test
        @DisplayName("should return bill payments for user")
        void shouldReturnBillPaymentsForUser() throws Exception {
            BillPaymentRequest request = new BillPaymentRequest(
                    TEST_USER_ID,
                    BillPayment.BillCategory.INTERNET,
                    "ISP Co",
                    "ISP-001",
                    "ACC-ISP",
                    "John Doe",
                    new BigDecimal("60.00"),
                    "USD",
                    "Internet bill"
            );
            mvc.post().uri("/api/v1/payments/bills")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(mvc.get().uri("/api/v1/payments/bills/user/{userId}?page=0&size=10", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Money Transfers")
    class MoneyTransfers {

        @Test
        @DisplayName("should initiate a money transfer")
        void shouldInitiateTransfer() throws Exception {
            UUID recipientId = UUID.randomUUID();
            MoneyTransferRequest request = new MoneyTransferRequest(
                    recipientId, new BigDecimal("100.00"), "USD", "Test transfer");

            MvcTestResult result = mvc.post().uri("/api/v1/payments/transfers")
                    .header("X-User-Id", TEST_USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson()
                    .extractingPath("$.senderUserId").isEqualTo(TEST_USER_ID.toString());
            assertThat(result).bodyJson()
                    .extractingPath("$.recipientUserId").isEqualTo(recipientId.toString());
            assertThat(result).bodyJson()
                    .extractingPath("$.transactionReference").asString().startsWith("TRF-");
        }

        @Test
        @DisplayName("should get transfer by ID")
        void shouldGetTransferById() throws Exception {
            UUID recipientId = UUID.randomUUID();
            MoneyTransferRequest request = new MoneyTransferRequest(
                    recipientId, new BigDecimal("50.00"), "USD", "Find me transfer");

            MvcTestResult createResult = mvc.post().uri("/api/v1/payments/transfers")
                    .header("X-User-Id", TEST_USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            String transferId = objectMapper.readTree(
                    createResult.getResponse().getContentAsString()
            ).get("id").asText();

            assertThat(mvc.get().uri("/api/v1/payments/transfers/{transferId}", transferId))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.description").isEqualTo("Find me transfer");
        }

        @Test
        @DisplayName("should get transfers for user (paginated)")
        void shouldGetTransfersForUser() throws Exception {
            UUID recipientId = UUID.randomUUID();
            MoneyTransferRequest request = new MoneyTransferRequest(
                    recipientId, new BigDecimal("30.00"), "USD", "User transfers");

            mvc.post().uri("/api/v1/payments/transfers")
                    .header("X-User-Id", TEST_USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(mvc.get().uri("/api/v1/payments/transfers/user/{userId}?page=0&size=10",
                    TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
        }

        @Test
        @DisplayName("should return 400 for null recipient")
        void shouldRejectNullRecipient() throws Exception {
            MoneyTransferRequest request = new MoneyTransferRequest(
                    null, new BigDecimal("50.00"), "USD", "No recipient");

            assertThat(mvc.post().uri("/api/v1/payments/transfers")
                    .header("X-User-Id", TEST_USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Money Requests")
    class MoneyRequests {

        @Test
        @DisplayName("should create a money request")
        void shouldCreateMoneyRequest() throws Exception {
            UUID payerId = UUID.randomUUID();
            MoneyRequestCreateDto request = new MoneyRequestCreateDto(
                    payerId, new BigDecimal("25.00"), "USD", "Lunch money");

            MvcTestResult result = mvc.post().uri("/api/v1/payments/requests")
                    .header("X-User-Id", TEST_USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson()
                    .extractingPath("$.requesterUserId").isEqualTo(TEST_USER_ID.toString());
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("PENDING_APPROVAL");
        }

        @Test
        @DisplayName("should get money request by ID")
        void shouldGetMoneyRequestById() throws Exception {
            UUID payerId = UUID.randomUUID();
            MoneyRequestCreateDto request = new MoneyRequestCreateDto(
                    payerId, new BigDecimal("30.00"), "USD", "Get me request");

            MvcTestResult createResult = mvc.post().uri("/api/v1/payments/requests")
                    .header("X-User-Id", TEST_USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            String requestId = objectMapper.readTree(
                    createResult.getResponse().getContentAsString()
            ).get("id").asText();

            assertThat(mvc.get().uri("/api/v1/payments/requests/{requestId}", requestId))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.description").isEqualTo("Get me request");
        }

        @Test
        @DisplayName("should decline a money request")
        void shouldDeclineMoneyRequest() throws Exception {
            UUID payerId = UUID.randomUUID();
            MoneyRequestCreateDto request = new MoneyRequestCreateDto(
                    payerId, new BigDecimal("50.00"), "USD", "Decline me");

            MvcTestResult createResult = mvc.post().uri("/api/v1/payments/requests")
                    .header("X-User-Id", TEST_USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            String requestId = objectMapper.readTree(
                    createResult.getResponse().getContentAsString()
            ).get("id").asText();

            assertThat(mvc.post().uri("/api/v1/payments/requests/{requestId}/decline", requestId)
                    .header("X-User-Id", payerId.toString()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.status").isEqualTo("DECLINED");
        }

        @Test
        @DisplayName("should cancel a money request by requester")
        void shouldCancelMoneyRequest() throws Exception {
            UUID payerId = UUID.randomUUID();
            MoneyRequestCreateDto request = new MoneyRequestCreateDto(
                    payerId, new BigDecimal("40.00"), "USD", "Cancel me");

            MvcTestResult createResult = mvc.post().uri("/api/v1/payments/requests")
                    .header("X-User-Id", TEST_USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            String requestId = objectMapper.readTree(
                    createResult.getResponse().getContentAsString()
            ).get("id").asText();

            assertThat(mvc.post().uri("/api/v1/payments/requests/{requestId}/cancel", requestId)
                    .header("X-User-Id", TEST_USER_ID.toString()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.status").isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should return pending request count")
        void shouldGetPendingRequestCount() {
            assertThat(mvc.get().uri("/api/v1/payments/requests/pending/count")
                    .header("X-User-Id", TEST_USER_ID.toString()))
                    .hasStatusOk();
        }

        @Test
        @DisplayName("should return pending incoming requests for payer")
        void shouldReturnPendingIncoming() throws Exception {
            UUID payerId = UUID.randomUUID();
            MoneyRequestCreateDto request = new MoneyRequestCreateDto(
                    payerId, new BigDecimal("15.00"), "USD", "Incoming request");

            mvc.post().uri("/api/v1/payments/requests")
                    .header("X-User-Id", TEST_USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(mvc.get().uri("/api/v1/payments/requests/pending/incoming?page=0&size=10")
                    .header("X-User-Id", payerId.toString()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
        }
    }
}
