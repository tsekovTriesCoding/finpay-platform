package com.finpay.user.service;

import com.finpay.user.config.KafkaConfig;
import com.finpay.user.event.UserEvent;
import com.finpay.user.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Kafka producer for user events.
 * 
 * Uses the Transactional Outbox Pattern: events are persisted to the
 * {@code outbox_events} table inside the caller's database transaction.
 * A background poller publishes them to Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final OutboxService outboxService;

    public void sendUserEvent(UserEvent event) {
        log.info("Saving user event to outbox: {} for user: {}", event.eventType(), event.userId());

        outboxService.saveEvent(
                "User",
                event.userId().toString(),
                event.eventType().name(),
                KafkaConfig.USER_EVENTS_TOPIC,
                event.userId().toString(),
                event
        );

        log.debug("User event saved to outbox: {} for user: {}", event.eventType(), event.userId());
    }
}
