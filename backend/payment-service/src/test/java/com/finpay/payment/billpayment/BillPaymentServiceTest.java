package com.finpay.payment.billpayment;

import com.finpay.payment.billpayment.dto.BillPaymentRequest;
import com.finpay.payment.billpayment.dto.BillPaymentResponse;
import com.finpay.payment.billpayment.event.BillPaymentEvent;
import com.finpay.payment.shared.exception.PaymentException;
import com.finpay.payment.shared.exception.ResourceNotFoundException;
import com.finpay.payment.shared.kafka.WalletCommandProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillPaymentService Unit Tests")
class BillPaymentServiceTest {

    @Mock private BillPaymentRepository billPaymentRepository;
    @Mock private BillPaymentEventProducer eventProducer;
    @Mock private WalletCommandProducer walletCommandProducer;

    @InjectMocks private BillPaymentService billPaymentService;

    private UUID userId;
    private UUID billId;
    private BillPayment testBill;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        billId = UUID.randomUUID();

        testBill = BillPayment.builder()
                .id(billId)
                .userId(userId)
                .transactionReference("BILL12345678ABCDE")
                .category(BillPayment.BillCategory.ELECTRICITY)
                .billerName("Power Corp")
                .billerCode("PWR001")
                .accountNumber("ACC-123456")
                .amount(new BigDecimal("150.0000"))
                .currency("USD")
                .processingFee(new BigDecimal("0.7500"))
                .totalAmount(new BigDecimal("150.7500"))
                .status(BillPayment.BillPaymentStatus.PENDING)
                .sagaStatus(BillPayment.SagaStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Initiate Bill Payment")
    class InitiateBillPaymentTests {

        @Test
        @DisplayName("should initiate bill payment and reserve funds")
        void shouldInitiateBillPayment() {
            BillPaymentRequest request = new BillPaymentRequest(
                    userId, BillPayment.BillCategory.ELECTRICITY, "Power Corp",
                    "PWR001", "ACC-123456", "John Doe",
                    new BigDecimal("150.00"), "USD", "Monthly electric bill"
            );

            when(billPaymentRepository.save(any(BillPayment.class))).thenReturn(testBill);

            BillPaymentResponse response = billPaymentService.initiateBillPayment(request);

            assertThat(response).isNotNull();
            verify(eventProducer).sendBillPaymentEvent(any(BillPaymentEvent.class));
            verify(walletCommandProducer).reserveFunds(eq(billId), eq(userId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }
    }

    @Nested
    @DisplayName("SAGA Steps")
    class SagaStepTests {

        @Test
        @DisplayName("should handle funds reserved")
        void shouldHandleFundsReserved() {
            UUID walletId = UUID.randomUUID();
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billPaymentRepository.save(any(BillPayment.class))).thenReturn(testBill);

            billPaymentService.handleFundsReserved(billId, walletId);

            assertThat(testBill.isFundsReserved()).isTrue();
            assertThat(testBill.getWalletId()).isEqualTo(walletId);
            assertThat(testBill.getSagaStatus()).isEqualTo(BillPayment.SagaStatus.FUNDS_RESERVED);
            verify(walletCommandProducer).deductFunds(eq(billId), eq(userId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }

        @Test
        @DisplayName("should handle funds deducted and confirm with biller")
        void shouldHandleFundsDeducted() {
            testBill.setFundsReserved(true);
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billPaymentRepository.save(any(BillPayment.class))).thenReturn(testBill);

            billPaymentService.handleFundsDeducted(billId);

            assertThat(testBill.isFundsDeducted()).isTrue();
            assertThat(testBill.isBillerConfirmed()).isTrue();
            assertThat(testBill.getStatus()).isEqualTo(BillPayment.BillPaymentStatus.COMPLETED);
            assertThat(testBill.getSagaStatus()).isEqualTo(BillPayment.SagaStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Failure and Compensation")
    class FailureTests {

        @Test
        @DisplayName("should handle failure and start compensation when funds reserved")
        void shouldHandleFailureWithReservation() {
            testBill.setFundsReserved(true);
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billPaymentRepository.save(any(BillPayment.class))).thenReturn(testBill);

            billPaymentService.handleFailure(billId, "Biller rejected");

            assertThat(testBill.getStatus()).isEqualTo(BillPayment.BillPaymentStatus.COMPENSATING);
            assertThat(testBill.isCompensationRequired()).isTrue();
            verify(walletCommandProducer).releaseFunds(eq(billId), eq(userId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }

        @Test
        @DisplayName("should handle failure and reverse deduction when funds deducted")
        void shouldHandleFailureWithDeduction() {
            testBill.setFundsReserved(true);
            testBill.setFundsDeducted(true);
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billPaymentRepository.save(any(BillPayment.class))).thenReturn(testBill);

            billPaymentService.handleFailure(billId, "Biller error");

            verify(walletCommandProducer).reverseDeduction(eq(billId), eq(userId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }

        @Test
        @DisplayName("should complete compensation without wallet ops when no funds reserved")
        void shouldCompleteCompensationDirectly() {
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billPaymentRepository.save(any(BillPayment.class))).thenReturn(testBill);

            billPaymentService.handleFailure(billId, "Early failure");

            assertThat(testBill.isCompensationCompleted()).isTrue();
            assertThat(testBill.getSagaStatus()).isEqualTo(BillPayment.SagaStatus.COMPENSATED);
            verifyNoInteractions(walletCommandProducer);
        }

        @Test
        @DisplayName("should handle compensated")
        void shouldHandleCompensated() {
            testBill.setCompensationRequired(true);
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billPaymentRepository.save(any(BillPayment.class))).thenReturn(testBill);

            billPaymentService.handleCompensated(billId);

            assertThat(testBill.isCompensationCompleted()).isTrue();
            assertThat(testBill.getStatus()).isEqualTo(BillPayment.BillPaymentStatus.COMPENSATED);
        }
    }

    @Nested
    @DisplayName("Cancel Bill Payment")
    class CancelTests {

        @Test
        @DisplayName("should cancel pending bill payment")
        void shouldCancelPending() {
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billPaymentRepository.save(any(BillPayment.class))).thenReturn(testBill);

            billPaymentService.cancelBillPayment(billId);

            assertThat(testBill.getStatus()).isEqualTo(BillPayment.BillPaymentStatus.CANCELLED);
            verify(eventProducer).sendBillPaymentEvent(any(BillPaymentEvent.class));
        }

        @Test
        @DisplayName("should throw when cancelling non-pending bill")
        void shouldThrowWhenNotPending() {
            testBill.setStatus(BillPayment.BillPaymentStatus.COMPLETED);
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.of(testBill));

            assertThatThrownBy(() -> billPaymentService.cancelBillPayment(billId))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("pending");
        }
    }

    @Nested
    @DisplayName("Get Operations")
    class GetOperations {

        @Test
        @DisplayName("should get bill payment by ID")
        void shouldGetById() {
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.of(testBill));

            BillPaymentResponse response = billPaymentService.getBillPayment(billId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(billId);
        }

        @Test
        @DisplayName("should throw for non-existent bill")
        void shouldThrowWhenNotFound() {
            when(billPaymentRepository.findById(billId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billPaymentService.getBillPayment(billId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should return all bill categories")
        void shouldReturnAllCategories() {
            BillPayment.BillCategory[] categories = billPaymentService.getAvailableCategories();

            assertThat(categories).contains(
                    BillPayment.BillCategory.ELECTRICITY,
                    BillPayment.BillCategory.WATER,
                    BillPayment.BillCategory.INTERNET
            );
        }
    }
}
