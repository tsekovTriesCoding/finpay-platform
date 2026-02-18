package com.finpay.auth.kafka;

import com.finpay.auth.config.KafkaConfig;
import com.finpay.auth.event.UserRegisteredEvent;
import com.finpay.auth.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for auth events.

 * Uses the Transactional Outbox Pattern: events are persisted to the
 * {@code outbox_events} table inside the caller's database transaction
 * instead of being sent directly to Kafka.  A background poller
 * ({@link com.finpay.auth.outbox.OutboxPublisher}) picks them up and
 * publishes them, guaranteeing at-least-once delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventProducer {

    private final OutboxService outboxService;

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Saving UserRegisteredEvent to outbox for user: {}", event.email());

        outboxService.saveEvent(
                "UserCredential",
                event.userId().toString(),
                "USER_REGISTERED",
                KafkaConfig.AUTH_EVENTS_TOPIC,
                event.userId().toString(),
                event
        );

        log.debug("UserRegisteredEvent saved to outbox for user: {}", event.email());
    }
}
