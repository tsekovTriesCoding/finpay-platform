package com.finpay.wallet.wallet.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.shared.event.UserEvent;
import com.finpay.outbox.idempotency.IdempotentConsumerService;
import com.finpay.wallet.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;

/**
 * Consumes user-events from Kafka to create/close wallets.
 *
 * Configured with non-blocking retries and a Dead Letter Topic (DLT):
 * - Up to 4 attempts (1 initial + 3 retries) with exponential backoff (1s, 2s, 4s)
 * - Transient exceptions (DB, network) are first retried in-place via blocking retries
 * - After all retries exhausted, messages are sent to user-events-dlt
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final WalletService walletService;
    private final ObjectMapper objectMapper;
    private final IdempotentConsumerService idempotentConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = "user-events", groupId = "wallet-service-user-consumer")
    public void handleUserEvent(String message,
                                @Header(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws Exception {
        if (idempotentConsumer.isDuplicate(idempotencyKey)) {
            log.info("Duplicate user event detected, skipping: idempotencyKey={}", idempotencyKey);
            return;
        }

        UserEvent event = objectMapper.readValue(message, UserEvent.class);
        log.info("Received user event: {} for userId: {}", event.eventType(), event.userId());

        switch (event.eventType()) {
            case USER_CREATED -> handleUserCreated(event);
            case USER_UPDATED, PLAN_UPGRADED -> handleUserUpdated(event);
            case USER_DELETED -> handleUserDeleted(event);
            default -> log.debug("Ignoring user event type: {}", event.eventType());
        }

        idempotentConsumer.markProcessed(idempotencyKey, "user-event-consumer");
    }

    /**
     * Dead Letter Topic handler for user-events that failed all retry attempts.
     * Logs the failed message for investigation and manual reprocessing.
     */
    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        log.error("DLT: Failed to process user event after all retries. Topic: {}, Key: {}, Value: {}, Error: {}",
                topic, record.key(), record.value(), errorMessage);
    }

    private void handleUserCreated(UserEvent event) {
        log.info("Creating wallet for new user: {} ({}) with plan: {}", event.userId(), event.email(), event.plan());
        walletService.getOrCreateWalletWithPlan(event.userId(), event.plan());
        log.info("Wallet created/verified for user: {} on plan: {}", event.userId(), event.plan());
    }

    private void handleUserUpdated(UserEvent event) {
        if (event.plan() == null || event.plan().isBlank()) {
            log.debug("USER_UPDATED event for user {} has no plan change, skipping wallet update", event.userId());
            return;
        }
        log.info("Upgrading wallet plan for user: {} to plan: {}", event.userId(), event.plan());
        walletService.upgradePlan(event.userId(), event.plan());
        log.info("Wallet plan upgraded for user: {} to {}", event.userId(), event.plan());
    }

    private void handleUserDeleted(UserEvent event) {
        log.info("Deactivating wallet for deleted user: {}", event.userId());
        walletService.closeWalletForUser(event.userId());
    }
}
