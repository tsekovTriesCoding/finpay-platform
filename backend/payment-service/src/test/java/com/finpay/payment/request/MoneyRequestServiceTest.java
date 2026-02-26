package com.finpay.payment.request;

import com.finpay.payment.request.dto.MoneyRequestCreateDto;
import com.finpay.payment.request.dto.MoneyRequestResponse;
import com.finpay.payment.request.event.MoneyRequestEvent;
import com.finpay.payment.shared.exception.ResourceNotFoundException;
import com.finpay.payment.shared.exception.TransferException;
import com.finpay.payment.transfer.MoneyTransfer;
import com.finpay.payment.transfer.MoneyTransferService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoneyRequestService Unit Tests")
class MoneyRequestServiceTest {

    @Mock private MoneyRequestRepository requestRepository;
    @Mock private MoneyTransferService moneyTransferService;
    @Mock private MoneyRequestEventProducer requestEventProducer;

    @InjectMocks private MoneyRequestService requestService;

    private UUID requesterId;
    private UUID payerId;
    private UUID requestId;
    private MoneyRequest testRequest;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        payerId = UUID.randomUUID();
        requestId = UUID.randomUUID();

        testRequest = MoneyRequest.builder()
                .id(requestId)
                .requestReference("REQ-12345-ABCDE")
                .requesterUserId(requesterId)
                .payerUserId(payerId)
                .amount(new BigDecimal("75.0000"))
                .currency("USD")
                .description("Dinner split")
                .status(MoneyRequest.RequestStatus.PENDING_APPROVAL)
                .sagaStatus(MoneyRequest.SagaStatus.NOT_STARTED)
                .fundsReserved(false)
                .fundsDeducted(false)
                .fundsCredited(false)
                .notificationSent(false)
                .compensationRequired(false)
                .compensationCompleted(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Create Request")
    class CreateRequestTests {

        @Test
        @DisplayName("should create money request successfully")
        void shouldCreateRequest() {
            MoneyRequestCreateDto dto = new MoneyRequestCreateDto(
                    payerId, new BigDecimal("75.00"), "USD", "Dinner split"
            );

            when(requestRepository.save(any(MoneyRequest.class))).thenReturn(testRequest);

            MoneyRequestResponse response = requestService.createRequest(requesterId, dto);

            assertThat(response).isNotNull();
            assertThat(response.requesterUserId()).isEqualTo(requesterId);
            assertThat(response.payerUserId()).isEqualTo(payerId);
            verify(requestEventProducer).publishRequestEvent(any(MoneyRequestEvent.class));
        }

        @Test
        @DisplayName("should throw when requesting from self")
        void shouldThrowWhenRequestingFromSelf() {
            MoneyRequestCreateDto dto = new MoneyRequestCreateDto(
                    requesterId, new BigDecimal("50.00"), "USD", "Self"
            );

            assertThatThrownBy(() -> requestService.createRequest(requesterId, dto))
                    .isInstanceOf(TransferException.class)
                    .hasMessageContaining("yourself");
        }

        @Test
        @DisplayName("should throw when amount is not positive")
        void shouldThrowWhenAmountNotPositive() {
            MoneyRequestCreateDto dto = new MoneyRequestCreateDto(
                    payerId, BigDecimal.ZERO, "USD", "Zero"
            );

            assertThatThrownBy(() -> requestService.createRequest(requesterId, dto))
                    .isInstanceOf(TransferException.class)
                    .hasMessageContaining("positive");
        }
    }

    @Nested
    @DisplayName("Approve Request")
    class ApproveRequestTests {

        @Test
        @DisplayName("should approve request and initiate transfer")
        void shouldApproveRequest() {
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
            when(requestRepository.save(any(MoneyRequest.class))).thenReturn(testRequest);
            MoneyTransfer mockTransfer = MoneyTransfer.builder()
                    .id(UUID.randomUUID())
                    .transactionReference("TRF-NEW")
                    .senderUserId(payerId)
                    .recipientUserId(requesterId)
                    .amount(new BigDecimal("75.0000"))
                    .currency("USD")
                    .status(MoneyTransfer.TransferStatus.PROCESSING)
                    .sagaStatus(MoneyTransfer.SagaStatus.STARTED)
                    .transferType(MoneyTransfer.TransferType.REQUEST_PAYMENT)
                    .fundsReserved(false).fundsDeducted(false).fundsCredit(false)
                    .notificationSent(false).compensationRequired(false).compensationCompleted(false)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(moneyTransferService.createTransferForRequest(eq(payerId), eq(requesterId),
                    any(BigDecimal.class), eq("USD"), anyString(), eq(requestId)))
                    .thenReturn(mockTransfer);

            MoneyRequestResponse response = requestService.approveRequest(payerId, requestId);

            assertThat(testRequest.getStatus()).isEqualTo(MoneyRequest.RequestStatus.PROCESSING);
            assertThat(testRequest.getSagaStatus()).isEqualTo(MoneyRequest.SagaStatus.STARTED);
            verify(moneyTransferService).createTransferForRequest(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should throw when non-payer tries to approve")
        void shouldThrowWhenNonPayerApproves() {
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));

            UUID randomUser = UUID.randomUUID();
            assertThatThrownBy(() -> requestService.approveRequest(randomUser, requestId))
                    .isInstanceOf(TransferException.class)
                    .hasMessageContaining("payer");
        }

        @Test
        @DisplayName("should throw when request not pending")
        void shouldThrowWhenNotPending() {
            testRequest.setStatus(MoneyRequest.RequestStatus.DECLINED);
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));

            assertThatThrownBy(() -> requestService.approveRequest(payerId, requestId))
                    .isInstanceOf(TransferException.class)
                    .hasMessageContaining("no longer pending");
        }
    }

    @Nested
    @DisplayName("Decline Request")
    class DeclineRequestTests {

        @Test
        @DisplayName("should decline request")
        void shouldDeclineRequest() {
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
            when(requestRepository.save(any(MoneyRequest.class))).thenReturn(testRequest);

            MoneyRequestResponse response = requestService.declineRequest(payerId, requestId);

            assertThat(testRequest.getStatus()).isEqualTo(MoneyRequest.RequestStatus.DECLINED);
            assertThat(testRequest.getDeclinedAt()).isNotNull();
            verify(requestEventProducer).publishRequestEvent(any(MoneyRequestEvent.class));
        }
    }

    @Nested
    @DisplayName("Cancel Request")
    class CancelRequestTests {

        @Test
        @DisplayName("should cancel request by requester")
        void shouldCancelRequest() {
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
            when(requestRepository.save(any(MoneyRequest.class))).thenReturn(testRequest);

            MoneyRequestResponse response = requestService.cancelRequest(requesterId, requestId);

            assertThat(testRequest.getStatus()).isEqualTo(MoneyRequest.RequestStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw when non-requester tries to cancel")
        void shouldThrowWhenNonRequesterCancels() {
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));

            assertThatThrownBy(() -> requestService.cancelRequest(payerId, requestId))
                    .isInstanceOf(TransferException.class)
                    .hasMessageContaining("requester");
        }

        @Test
        @DisplayName("should throw when cancelling non-pending request")
        void shouldThrowWhenCancellingNonPending() {
            testRequest.setStatus(MoneyRequest.RequestStatus.COMPLETED);
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));

            assertThatThrownBy(() -> requestService.cancelRequest(requesterId, requestId))
                    .isInstanceOf(TransferException.class)
                    .hasMessageContaining("pending");
        }
    }

    @Nested
    @DisplayName("Fail and Complete Request")
    class FailCompleteTests {

        @Test
        @DisplayName("should fail request")
        void shouldFailRequest() {
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
            when(requestRepository.save(any(MoneyRequest.class))).thenReturn(testRequest);

            requestService.failRequest(requestId, "Insufficient funds");

            assertThat(testRequest.getStatus()).isEqualTo(MoneyRequest.RequestStatus.FAILED);
            assertThat(testRequest.getFailureReason()).isEqualTo("Insufficient funds");
            assertThat(testRequest.isCompensationRequired()).isTrue();
        }

        @Test
        @DisplayName("should complete request")
        void shouldCompleteRequest() {
            UUID payerWallet = UUID.randomUUID();
            UUID requesterWallet = UUID.randomUUID();
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
            when(requestRepository.save(any(MoneyRequest.class))).thenReturn(testRequest);

            requestService.completeRequest(requestId, payerWallet, requesterWallet);

            assertThat(testRequest.getStatus()).isEqualTo(MoneyRequest.RequestStatus.COMPLETED);
            assertThat(testRequest.isFundsReserved()).isTrue();
            assertThat(testRequest.isFundsDeducted()).isTrue();
            assertThat(testRequest.isFundsCredited()).isTrue();
        }
    }
}
