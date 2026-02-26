package com.finpay.payment.payment;

import com.finpay.payment.payment.dto.PaymentRequest;
import com.finpay.payment.payment.dto.PaymentResponse;
import com.finpay.payment.payment.event.PaymentEvent;
import com.finpay.payment.shared.exception.PaymentException;
import com.finpay.payment.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentEventProducer paymentEventProducer;
    @Mock private PaymentMapper paymentMapper;
    @Mock private TaskExecutor taskExecutor;

    private PaymentService paymentService;

    private UUID userId;
    private UUID paymentId;
    private Payment testPayment;
    private PaymentResponse testResponse;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentEventProducer, paymentMapper, taskExecutor);

        userId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        testPayment = Payment.builder()
                .id(paymentId)
                .userId(userId)
                .transactionReference("FP12345678ABCDEF01")
                .amount(new BigDecimal("100.0000"))
                .currency("USD")
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(Payment.PaymentMethod.CARD)
                .paymentType(Payment.PaymentType.PAYMENT)
                .processingFee(new BigDecimal("1.5000"))
                .totalAmount(new BigDecimal("101.5000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testResponse = new PaymentResponse(
                paymentId, userId, "FP12345678ABCDEF01",
                new BigDecimal("100.0000"), "USD",
                Payment.PaymentStatus.PENDING, Payment.PaymentMethod.CARD,
                Payment.PaymentType.PAYMENT, "Test payment",
                null, null, null, null, null,
                new BigDecimal("1.5000"), new BigDecimal("101.5000"),
                null, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("Initiate Payment")
    class InitiatePaymentTests {

        @Test
        @DisplayName("should initiate payment with processing fee calculation")
        void shouldInitiatePayment() {
            PaymentRequest request = new PaymentRequest(
                    userId, new BigDecimal("100.00"), "usd",
                    Payment.PaymentMethod.CARD, Payment.PaymentType.PAYMENT,
                    "Test payment", null, null, null, null,
                    "4111111111111111", "12", "25", "123", "John Doe"
            );

            when(paymentMapper.toEntity(request)).thenReturn(testPayment);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(paymentMapper.toResponse(testPayment)).thenReturn(testResponse);
            when(paymentMapper.toEvent(any(Payment.class), any(PaymentEvent.EventType.class)))
                    .thenReturn(mock(PaymentEvent.class));

            PaymentResponse response = paymentService.initiatePayment(request);

            assertThat(response).isNotNull();
            verify(paymentRepository).save(any(Payment.class));
            verify(paymentEventProducer).sendPaymentEvent(any(PaymentEvent.class));
        }

        @Test
        @DisplayName("should set card last four digits and detect card type")
        void shouldSetCardDetails() {
            PaymentRequest request = new PaymentRequest(
                    userId, new BigDecimal("50.00"), "USD",
                    Payment.PaymentMethod.CARD, Payment.PaymentType.PAYMENT,
                    "Card payment", null, null, null, null,
                    "4111111111111111", "12", "25", "123", "John Doe"
            );

            Payment capturedPayment = Payment.builder()
                    .userId(userId).amount(new BigDecimal("50.00"))
                    .status(Payment.PaymentStatus.PENDING)
                    .paymentMethod(Payment.PaymentMethod.CARD)
                    .paymentType(Payment.PaymentType.PAYMENT)
                    .build();

            when(paymentMapper.toEntity(request)).thenReturn(capturedPayment);
            when(paymentRepository.save(any(Payment.class))).thenReturn(capturedPayment);
            when(paymentMapper.toResponse(any())).thenReturn(testResponse);
            when(paymentMapper.toEvent(any(), any())).thenReturn(mock(PaymentEvent.class));

            paymentService.initiatePayment(request);

            assertThat(capturedPayment.getCardLastFourDigits()).isEqualTo("1111");
            assertThat(capturedPayment.getCardType()).isEqualTo("VISA");
        }

        @Test
        @DisplayName("should set currency to uppercase")
        void shouldSetCurrencyUppercase() {
            PaymentRequest request = new PaymentRequest(
                    userId, new BigDecimal("25.00"), "eur",
                    Payment.PaymentMethod.WALLET, Payment.PaymentType.DEPOSIT,
                    null, null, null, null, null,
                    null, null, null, null, null
            );

            Payment capturedPayment = Payment.builder()
                    .userId(userId).amount(new BigDecimal("25.00"))
                    .status(Payment.PaymentStatus.PENDING)
                    .paymentMethod(Payment.PaymentMethod.WALLET)
                    .paymentType(Payment.PaymentType.DEPOSIT)
                    .build();

            when(paymentMapper.toEntity(request)).thenReturn(capturedPayment);
            when(paymentRepository.save(any(Payment.class))).thenReturn(capturedPayment);
            when(paymentMapper.toResponse(any())).thenReturn(testResponse);
            when(paymentMapper.toEvent(any(), any())).thenReturn(mock(PaymentEvent.class));

            paymentService.initiatePayment(request);

            assertThat(capturedPayment.getCurrency()).isEqualTo("EUR");
        }
    }

    @Nested
    @DisplayName("Process Payment")
    class ProcessPaymentTests {

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenNotFound() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processPayment(paymentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when payment is not PENDING")
        void shouldThrowWhenNotPending() {
            testPayment.setStatus(Payment.PaymentStatus.COMPLETED);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

            assertThatThrownBy(() -> paymentService.processPayment(paymentId))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("not in PENDING status");
        }
    }

    @Nested
    @DisplayName("Cancel Payment")
    class CancelPaymentTests {

        @Test
        @DisplayName("should cancel pending payment")
        void shouldCancelPendingPayment() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(paymentMapper.toResponse(testPayment)).thenReturn(testResponse);
            when(paymentMapper.toEvent(any(), any())).thenReturn(mock(PaymentEvent.class));

            paymentService.cancelPayment(paymentId);

            assertThat(testPayment.getStatus()).isEqualTo(Payment.PaymentStatus.CANCELLED);
            verify(paymentEventProducer).sendPaymentEvent(any(PaymentEvent.class));
        }

        @Test
        @DisplayName("should throw when cancelling non-pending payment")
        void shouldThrowWhenCancellingNonPending() {
            testPayment.setStatus(Payment.PaymentStatus.COMPLETED);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

            assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("pending payments can be cancelled");
        }

        @Test
        @DisplayName("should throw when payment not found for cancel")
        void shouldThrowWhenNotFoundForCancel() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Refund Payment")
    class RefundPaymentTests {

        @Test
        @DisplayName("should refund completed payment")
        void shouldRefundCompletedPayment() {
            testPayment.setStatus(Payment.PaymentStatus.COMPLETED);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(paymentMapper.toResponse(testPayment)).thenReturn(testResponse);
            when(paymentMapper.toEvent(any(), any())).thenReturn(mock(PaymentEvent.class));

            paymentService.refundPayment(paymentId);

            assertThat(testPayment.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
            verify(paymentEventProducer).sendPaymentEvent(any(PaymentEvent.class));
        }

        @Test
        @DisplayName("should throw when refunding non-completed payment")
        void shouldThrowWhenRefundingNonCompleted() {
            testPayment.setStatus(Payment.PaymentStatus.PENDING);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

            assertThatThrownBy(() -> paymentService.refundPayment(paymentId))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("completed payments can be refunded");
        }
    }

    @Nested
    @DisplayName("Get Payments")
    class GetPaymentTests {

        @Test
        @DisplayName("should get payment by ID")
        void shouldGetById() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
            when(paymentMapper.toResponse(testPayment)).thenReturn(testResponse);

            PaymentResponse response = paymentService.getPaymentById(paymentId);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should get payment by reference")
        void shouldGetByReference() {
            when(paymentRepository.findByTransactionReference("FP12345678ABCDEF01"))
                    .thenReturn(Optional.of(testPayment));
            when(paymentMapper.toResponse(testPayment)).thenReturn(testResponse);

            PaymentResponse response = paymentService.getPaymentByReference("FP12345678ABCDEF01");

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should get payments by user ID")
        void shouldGetByUserId() {
            when(paymentRepository.findByUserId(userId)).thenReturn(List.of(testPayment));
            when(paymentMapper.toResponse(testPayment)).thenReturn(testResponse);

            List<PaymentResponse> responses = paymentService.getPaymentsByUserId(userId);

            assertThat(responses).hasSize(1);
        }

        @Test
        @DisplayName("should throw when payment not found by reference")
        void shouldThrowWhenNotFoundByRef() {
            when(paymentRepository.findByTransactionReference("NONE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByReference("NONE"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
