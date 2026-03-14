package com.finpay.auth.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.auth.entity.UserCredential;
import com.finpay.auth.repository.RefreshTokenRepository;
import com.finpay.auth.repository.UserCredentialRepository;
import com.finpay.auth.service.UserSessionCacheService;
import com.finpay.outbox.idempotency.IdempotentConsumerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventConsumer Unit Tests")
class UserEventConsumerTest {

    @Mock private UserCredentialRepository credentialRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Spy  private ObjectMapper objectMapper;
    @Mock private IdempotentConsumerService idempotentConsumer;
    @Mock private UserSessionCacheService sessionCacheService;

    @InjectMocks private UserEventConsumer consumer;

    private UUID userId;
    private UserCredential credential;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        credential = UserCredential.builder()
                .id(userId)
                .email("user@example.com")
                .enabled(true)
                .build();
    }

    private String statusChangedMessage(UUID uid, String status) {
        return """
                {"userId":"%s","email":"user@example.com","status":"%s","eventType":"USER_STATUS_CHANGED"}
                """.formatted(uid, status).strip();
    }

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("should skip duplicate events")
        void shouldSkipDuplicateEvents() throws Exception {
            when(idempotentConsumer.isDuplicate("key-1")).thenReturn(true);

            consumer.handleUserEvent("{}", "key-1", "USER_STATUS_CHANGED");

            verify(credentialRepository, never()).findById(any());
            verify(idempotentConsumer, never()).markProcessed(anyString(), anyString());
        }

        @Test
        @DisplayName("should mark event as processed after handling")
        void shouldMarkProcessedAfterHandling() throws Exception {
            when(idempotentConsumer.isDuplicate("key-2")).thenReturn(false);

            String message = statusChangedMessage(userId, "SUSPENDED");
            when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));
            when(credentialRepository.save(any())).thenReturn(credential);

            consumer.handleUserEvent(message, "key-2", "USER_STATUS_CHANGED");

            verify(idempotentConsumer).markProcessed("key-2", "user-event-consumer");
        }
    }

    @Nested
    @DisplayName("Event Routing")
    class EventRoutingTests {

        @Test
        @DisplayName("should ignore non-status-changed events")
        void shouldIgnoreNonStatusChangedEvents() throws Exception {
            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);

            consumer.handleUserEvent("{}", "key-3", "USER_CREATED");

            verify(credentialRepository, never()).findById(any());
            verify(idempotentConsumer).markProcessed("key-3", "user-event-consumer");
        }

        @Test
        @DisplayName("should handle null event type gracefully")
        void shouldHandleNullEventType() throws Exception {
            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);

            consumer.handleUserEvent("{}", "key-4", null);

            verify(credentialRepository, never()).findById(any());
            verify(idempotentConsumer).markProcessed("key-4", "user-event-consumer");
        }
    }

    @Nested
    @DisplayName("Status Changed - Suspend")
    class SuspendTests {

        @Test
        @DisplayName("should disable credential when user is suspended")
        void shouldDisableCredentialOnSuspend() throws Exception {
            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);
            when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));
            when(credentialRepository.save(any())).thenReturn(credential);

            consumer.handleUserEvent(
                    statusChangedMessage(userId, "SUSPENDED"), "key-5", "USER_STATUS_CHANGED");

            assertThat(credential.isEnabled()).isFalse();
            verify(credentialRepository).save(credential);
        }

        @Test
        @DisplayName("should revoke all refresh tokens when user is suspended")
        void shouldRevokeTokensOnSuspend() throws Exception {
            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);
            when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));
            when(credentialRepository.save(any())).thenReturn(credential);

            consumer.handleUserEvent(
                    statusChangedMessage(userId, "SUSPENDED"), "key-6", "USER_STATUS_CHANGED");

            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("should disable credential when user is set to INACTIVE")
        void shouldDisableCredentialOnInactive() throws Exception {
            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);
            when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));
            when(credentialRepository.save(any())).thenReturn(credential);

            consumer.handleUserEvent(
                    statusChangedMessage(userId, "INACTIVE"), "key-7", "USER_STATUS_CHANGED");

            assertThat(credential.isEnabled()).isFalse();
            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("should not update if credential is already disabled")
        void shouldNotUpdateIfAlreadyDisabled() throws Exception {
            credential.setEnabled(false);

            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);
            when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));

            consumer.handleUserEvent(
                    statusChangedMessage(userId, "SUSPENDED"), "key-8", "USER_STATUS_CHANGED");

            verify(credentialRepository, never()).save(any());
            verify(refreshTokenRepository, never()).revokeAllByUserId(any());
        }
    }

    @Nested
    @DisplayName("Status Changed - Unsuspend")
    class UnsuspendTests {

        @Test
        @DisplayName("should re-enable credential when user is set to ACTIVE")
        void shouldEnableCredentialOnActive() throws Exception {
            credential.setEnabled(false);

            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);
            when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));
            when(credentialRepository.save(any())).thenReturn(credential);

            consumer.handleUserEvent(
                    statusChangedMessage(userId, "ACTIVE"), "key-9", "USER_STATUS_CHANGED");

            assertThat(credential.isEnabled()).isTrue();
            verify(credentialRepository).save(credential);
        }

        @Test
        @DisplayName("should not revoke tokens when re-enabling a user")
        void shouldNotRevokeTokensOnUnsuspend() throws Exception {
            credential.setEnabled(false);

            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);
            when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));
            when(credentialRepository.save(any())).thenReturn(credential);

            consumer.handleUserEvent(
                    statusChangedMessage(userId, "ACTIVE"), "key-10", "USER_STATUS_CHANGED");

            verify(refreshTokenRepository, never()).revokeAllByUserId(any());
        }

        @Test
        @DisplayName("should not update if credential is already enabled")
        void shouldNotUpdateIfAlreadyEnabled() throws Exception {
            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);
            when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));

            consumer.handleUserEvent(
                    statusChangedMessage(userId, "ACTIVE"), "key-11", "USER_STATUS_CHANGED");

            verify(credentialRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should skip when credential is not found")
        void shouldSkipWhenCredentialNotFound() throws Exception {
            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);
            when(credentialRepository.findById(userId)).thenReturn(Optional.empty());

            consumer.handleUserEvent(
                    statusChangedMessage(userId, "SUSPENDED"), "key-12", "USER_STATUS_CHANGED");

            verify(credentialRepository, never()).save(any());
            verify(refreshTokenRepository, never()).revokeAllByUserId(any());
            // Event should still be marked processed to avoid re-processing
            verify(idempotentConsumer).markProcessed("key-12", "user-event-consumer");
        }

        @Test
        @DisplayName("should skip when status field is missing from event")
        void shouldSkipWhenStatusFieldMissing() throws Exception {
            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);

            String messageWithoutStatus = """
                    {"userId":"%s","email":"user@example.com","eventType":"USER_STATUS_CHANGED"}
                    """.formatted(userId).strip();

            consumer.handleUserEvent(messageWithoutStatus, "key-13", "USER_STATUS_CHANGED");

            verify(credentialRepository, never()).save(any());
            verify(idempotentConsumer).markProcessed("key-13", "user-event-consumer");
        }

        @Test
        @DisplayName("should propagate exception on malformed JSON")
        void shouldPropagateExceptionOnMalformedJson() {
            when(idempotentConsumer.isDuplicate(any())).thenReturn(false);

            assertThatThrownBy(() ->
                    consumer.handleUserEvent("not-json", "key-14", "USER_STATUS_CHANGED"))
                    .isInstanceOf(Exception.class);

            // Event should NOT be marked processed so retry picks it up
            verify(idempotentConsumer, never()).markProcessed(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("DLT Handler")
    class DltHandlerTests {

        @Test
        @DisplayName("should log DLT message without throwing")
        void shouldLogDltWithoutThrowing() {
            ConsumerRecord<String, String> record =
                    new ConsumerRecord<>("user-events-dlt", 0, 0, "key", "value");

            // Should not throw
            consumer.handleDlt(record, "user-events-dlt", "Deserialization error");
        }
    }
}
