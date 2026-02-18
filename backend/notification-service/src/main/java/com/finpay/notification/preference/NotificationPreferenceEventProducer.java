package com.finpay.notification.preference;

import com.finpay.notification.preference.event.NotificationPreferenceEvent;
import com.finpay.notification.shared.config.KafkaMessageConfig;
import com.finpay.notification.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Publishes notification preference events via the Transactional Outbox Pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceEventProducer {

    private final OutboxService outboxService;

    public void sendPreferenceEvent(NotificationPreferenceEvent event) {
        log.info("Saving notification preference event to outbox: {} for user: {}",
                event.eventType(), event.userId());

        outboxService.saveEvent(
                "NotificationPreference",
                event.userId().toString(),
                event.eventType(),
                KafkaMessageConfig.NOTIFICATION_PREFERENCE_EVENTS_TOPIC,
                event.userId().toString(),
                event
        );
    }
}
