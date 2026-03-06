package com.finpay.auth.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.auth.config.KafkaConfig;
import com.finpay.auth.entity.UserCredential;
import com.finpay.auth.repository.RefreshTokenRepository;
import com.finpay.auth.repository.UserCredentialRepository;
import com.finpay.outbox.idempotency.IdempotentConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consumes user events from user-service via Kafka.
 *
 * Reacts to {@code USER_STATUS_CHANGED} events: when a user is
 * suspended or un-suspended by an admin, the local
 * {@link UserCredential#isEnabled()} flag is updated so that
 * subsequent login attempts are checked locally — no cross-service
 * HTTP call required.
 *
 * Configured with non-blocking retries and DLT:
 * 4 attempts with exponential backoff (1 s → 2 s → 4 s).
 * Failed messages land in {@code user-events-dlt}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;
    private final IdempotentConsumerService idempotentConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = KafkaConfig.USER_EVENTS_TOPIC, groupId = "auth-service")
    @Transactional
    public void handleUserEvent(String message,
                                @Header(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                @Header(value = "X-Event-Type", required = false) String eventType) throws Exception {

        if (idempotentConsumer.isDuplicate(idempotencyKey)) {
            log.info("Duplicate user event detected, skipping: idempotencyKey={}", idempotencyKey);
            return;
        }

        log.info("Received user event: type={}", eventType);

        if ("USER_STATUS_CHANGED".equals(eventType)) {
            handleStatusChanged(message);
        } else {
            log.debug("Ignoring user event type: {}", eventType);
        }

        idempotentConsumer.markProcessed(idempotencyKey, "user-event-consumer");
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        log.error("DLT: Failed to process user event after all retries. Topic: {}, Key: {}, Value: {}, Error: {}",
                topic, record.key(), record.value(), errorMessage);
    }

    // Internal

    private void handleStatusChanged(String message) throws Exception {
        JsonNode node = objectMapper.readTree(message);

        UUID userId = UUID.fromString(node.get("userId").asText());
        String newStatus = node.has("status") ? node.get("status").asText() : null;

        if (newStatus == null) {
            log.warn("USER_STATUS_CHANGED event for user {} missing 'status' field, skipping", userId);
            return;
        }

        UserCredential credential = credentialRepository.findById(userId).orElse(null);
        if (credential == null) {
            log.warn("No credential found for userId={}, skipping status sync", userId);
            return;
        }

        boolean shouldBeEnabled = !"SUSPENDED".equalsIgnoreCase(newStatus)
                && !"INACTIVE".equalsIgnoreCase(newStatus);

        if (credential.isEnabled() != shouldBeEnabled) {
            credential.setEnabled(shouldBeEnabled);
            credentialRepository.save(credential);
            log.info("Synced credential enabled={} for user {} (status={})",
                    shouldBeEnabled, userId, newStatus);

            // Revoke all refresh tokens so existing sessions can't get new access tokens
            if (!shouldBeEnabled) {
                refreshTokenRepository.revokeAllByUserId(userId);
                log.info("Revoked all refresh tokens for suspended user {}", userId);
            }
        } else {
            log.debug("Credential enabled flag already correct for user {} (status={})", userId, newStatus);
        }
    }
}
