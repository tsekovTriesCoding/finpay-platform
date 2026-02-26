package com.finpay.payment.transfer;

import com.finpay.payment.shared.exception.ResourceNotFoundException;
import com.finpay.payment.shared.exception.TransferException;
import com.finpay.payment.shared.kafka.WalletCommandProducer;
import com.finpay.payment.transfer.dto.MoneyTransferRequest;
import com.finpay.payment.transfer.dto.MoneyTransferResponse;
import com.finpay.payment.transfer.event.TransferSagaEvent;
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
@DisplayName("MoneyTransferService Unit Tests")
class MoneyTransferServiceTest {

    @Mock private MoneyTransferRepository transferRepository;
    @Mock private WalletCommandProducer walletCommandProducer;
    @Mock private TransferSagaEventProducer sagaEventProducer;

    @InjectMocks private MoneyTransferService transferService;

    private UUID senderId;
    private UUID recipientId;
    private UUID transferId;
    private MoneyTransfer testTransfer;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        transferId = UUID.randomUUID();

        testTransfer = MoneyTransfer.builder()
                .id(transferId)
                .transactionReference("TRF-12345-ABCDE")
                .senderUserId(senderId)
                .recipientUserId(recipientId)
                .amount(new BigDecimal("50.0000"))
                .currency("USD")
                .description("Test transfer")
                .transferType(MoneyTransfer.TransferType.SEND)
                .status(MoneyTransfer.TransferStatus.PROCESSING)
                .sagaStatus(MoneyTransfer.SagaStatus.STARTED)
                .fundsReserved(false)
                .fundsDeducted(false)
                .fundsCredit(false)
                .notificationSent(false)
                .compensationRequired(false)
                .compensationCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Initiate Transfer")
    class InitiateTransferTests {

        @Test
        @DisplayName("should initiate transfer successfully")
        void shouldInitiateTransfer() {
            MoneyTransferRequest request = new MoneyTransferRequest(
                    recipientId, new BigDecimal("50.00"), "USD", "Lunch money"
            );

            when(transferRepository.save(any(MoneyTransfer.class))).thenReturn(testTransfer);

            MoneyTransferResponse response = transferService.initiateTransfer(senderId, request);

            assertThat(response).isNotNull();
            assertThat(response.senderUserId()).isEqualTo(senderId);
            verify(sagaEventProducer).sendSagaEvent(any(TransferSagaEvent.class));
            verify(walletCommandProducer).reserveFunds(eq(transferId), eq(senderId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }

        @Test
        @DisplayName("should throw when transferring to self")
        void shouldThrowWhenTransferringToSelf() {
            MoneyTransferRequest request = new MoneyTransferRequest(
                    senderId, new BigDecimal("50.00"), "USD", "Self"
            );

            assertThatThrownBy(() -> transferService.initiateTransfer(senderId, request))
                    .isInstanceOf(TransferException.class)
                    .hasMessageContaining("yourself");
        }

        @Test
        @DisplayName("should throw when amount is zero or negative")
        void shouldThrowWhenAmountInvalid() {
            MoneyTransferRequest request = new MoneyTransferRequest(
                    recipientId, BigDecimal.ZERO, "USD", "Zero"
            );

            assertThatThrownBy(() -> transferService.initiateTransfer(senderId, request))
                    .isInstanceOf(TransferException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("should throw when amount is negative")
        void shouldThrowWhenAmountNegative() {
            MoneyTransferRequest request = new MoneyTransferRequest(
                    recipientId, new BigDecimal("-10.00"), "USD", "Negative"
            );

            assertThatThrownBy(() -> transferService.initiateTransfer(senderId, request))
                    .isInstanceOf(TransferException.class)
                    .hasMessageContaining("positive");
        }
    }

    @Nested
    @DisplayName("SAGA Step Handlers")
    class SagaStepTests {

        @Test
        @DisplayName("should handle funds reserved - Step 1")
        void shouldHandleFundsReserved() {
            UUID walletId = UUID.randomUUID();
            when(transferRepository.findById(transferId)).thenReturn(Optional.of(testTransfer));
            when(transferRepository.save(any(MoneyTransfer.class))).thenReturn(testTransfer);

            transferService.handleFundsReserved(transferId, walletId);

            assertThat(testTransfer.isFundsReserved()).isTrue();
            assertThat(testTransfer.getSagaStatus()).isEqualTo(MoneyTransfer.SagaStatus.FUNDS_RESERVED);
            assertThat(testTransfer.getSenderWalletId()).isEqualTo(walletId);
            verify(walletCommandProducer).deductFunds(eq(transferId), eq(senderId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }

        @Test
        @DisplayName("should handle funds deducted - Step 2")
        void shouldHandleFundsDeducted() {
            testTransfer.setFundsReserved(true);
            testTransfer.setSagaStatus(MoneyTransfer.SagaStatus.FUNDS_RESERVED);
            when(transferRepository.findById(transferId)).thenReturn(Optional.of(testTransfer));
            when(transferRepository.save(any(MoneyTransfer.class))).thenReturn(testTransfer);

            transferService.handleFundsDeducted(transferId);

            assertThat(testTransfer.isFundsDeducted()).isTrue();
            assertThat(testTransfer.getSagaStatus()).isEqualTo(MoneyTransfer.SagaStatus.FUNDS_DEDUCTED);
            verify(walletCommandProducer).creditFunds(eq(transferId), eq(recipientId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }

        @Test
        @DisplayName("should handle funds credited - Step 3 and complete SAGA")
        void shouldHandleFundsCredited() {
            UUID recipientWalletId = UUID.randomUUID();
            testTransfer.setFundsReserved(true);
            testTransfer.setFundsDeducted(true);
            when(transferRepository.findById(transferId)).thenReturn(Optional.of(testTransfer));
            when(transferRepository.save(any(MoneyTransfer.class))).thenReturn(testTransfer);

            TransferSagaStepResult result = transferService.handleFundsCredited(transferId, recipientWalletId);

            assertThat(testTransfer.isFundsCredit()).isTrue();
            assertThat(testTransfer.isNotificationSent()).isTrue();
            assertThat(testTransfer.getStatus()).isEqualTo(MoneyTransfer.TransferStatus.COMPLETED);
            assertThat(testTransfer.getSagaStatus()).isEqualTo(MoneyTransfer.SagaStatus.COMPLETED);
            assertThat(testTransfer.getCompletedAt()).isNotNull();
            verify(sagaEventProducer).sendNotificationEvent(any(TransferSagaEvent.class));
        }
    }

    @Nested
    @DisplayName("SAGA Failure and Compensation")
    class SagaFailureTests {

        @Test
        @DisplayName("should handle saga failure")
        void shouldHandleSagaFailure() {
            when(transferRepository.findById(transferId)).thenReturn(Optional.of(testTransfer));
            when(transferRepository.save(any(MoneyTransfer.class))).thenReturn(testTransfer);

            TransferSagaStepResult result = transferService.handleSagaFailure(transferId, "Insufficient funds");

            assertThat(testTransfer.getStatus()).isEqualTo(MoneyTransfer.TransferStatus.COMPENSATED);
            assertThat(testTransfer.getSagaStatus()).isEqualTo(MoneyTransfer.SagaStatus.COMPENSATED);
            assertThat(testTransfer.getFailureReason()).isEqualTo("Insufficient funds");
            assertThat(testTransfer.isCompensationRequired()).isTrue();
            assertThat(testTransfer.isCompensationCompleted()).isTrue();
        }

        @Test
        @DisplayName("should start compensation with release when only reserved")
        void shouldCompensateWithRelease() {
            testTransfer.setFundsReserved(true);
            when(transferRepository.findById(transferId)).thenReturn(Optional.of(testTransfer));
            when(transferRepository.save(any(MoneyTransfer.class))).thenReturn(testTransfer);

            transferService.handleSagaFailure(transferId, "Credit failed");

            verify(walletCommandProducer).releaseFunds(eq(transferId), eq(senderId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }

        @Test
        @DisplayName("should start compensation with deduction reversal when funds deducted")
        void shouldCompensateWithDeductionReversal() {
            testTransfer.setFundsReserved(true);
            testTransfer.setFundsDeducted(true);
            when(transferRepository.findById(transferId)).thenReturn(Optional.of(testTransfer));
            when(transferRepository.save(any(MoneyTransfer.class))).thenReturn(testTransfer);

            transferService.handleSagaFailure(transferId, "Credit failed");

            verify(walletCommandProducer).reverseDeduction(eq(transferId), eq(senderId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }

        @Test
        @DisplayName("should start compensation with credit reversal when funds credited")
        void shouldCompensateWithCreditReversal() {
            testTransfer.setFundsReserved(true);
            testTransfer.setFundsDeducted(true);
            testTransfer.setFundsCredit(true);
            when(transferRepository.findById(transferId)).thenReturn(Optional.of(testTransfer));
            when(transferRepository.save(any(MoneyTransfer.class))).thenReturn(testTransfer);

            transferService.handleSagaFailure(transferId, "Notification failed");

            verify(walletCommandProducer).reverseCredit(eq(transferId), eq(recipientId),
                    any(BigDecimal.class), eq("USD"), anyString());
        }

        @Test
        @DisplayName("should handle funds released during compensation")
        void shouldHandleFundsReleased() {
            testTransfer.setFundsReserved(true);
            testTransfer.setCompensationRequired(true);
            when(transferRepository.findById(transferId)).thenReturn(Optional.of(testTransfer));
            when(transferRepository.save(any(MoneyTransfer.class))).thenReturn(testTransfer);

            transferService.handleFundsReleased(transferId);

            assertThat(testTransfer.isCompensationCompleted()).isTrue();
            assertThat(testTransfer.getSagaStatus()).isEqualTo(MoneyTransfer.SagaStatus.COMPENSATED);
            assertThat(testTransfer.getStatus()).isEqualTo(MoneyTransfer.TransferStatus.COMPENSATED);
        }
    }

    @Nested
    @DisplayName("Get Transfer")
    class GetTransferTests {

        @Test
        @DisplayName("should get transfer by ID")
        void shouldGetById() {
            when(transferRepository.findById(transferId)).thenReturn(Optional.of(testTransfer));

            MoneyTransferResponse response = transferService.getTransferById(transferId);

            assertThat(response.id()).isEqualTo(transferId);
        }

        @Test
        @DisplayName("should throw when transfer not found")
        void shouldThrowWhenNotFound() {
            when(transferRepository.findById(transferId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.getTransferById(transferId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should get transfer by reference")
        void shouldGetByReference() {
            when(transferRepository.findByTransactionReference("TRF-12345-ABCDE"))
                    .thenReturn(Optional.of(testTransfer));

            MoneyTransferResponse response = transferService.getTransferByReference("TRF-12345-ABCDE");

            assertThat(response.transactionReference()).isEqualTo("TRF-12345-ABCDE");
        }
    }
}
