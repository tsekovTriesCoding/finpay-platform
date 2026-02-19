package com.finpay.payment.request;

import com.finpay.payment.shared.config.KafkaConfig;
import com.finpay.outbox.service.OutboxService;
import com.finpay.payment.request.event.MoneyRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publishes money request lifecycle events via the Transactional Outbox Pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MoneyRequestEventProducer {

    private final OutboxService outboxService;

    public void publishRequestEvent(MoneyRequestEvent event) {
        log.info("Saving money request event to outbox: {} for request: {}",
                event.eventType(), event.requestId());

        outboxService.saveEvent(
                "MoneyRequest",
                event.requestId().toString(),
                event.eventType().name(),
                KafkaConfig.MONEY_REQUEST_EVENTS_TOPIC,
                event.requestId().toString(),
                event
        );
    }
}
