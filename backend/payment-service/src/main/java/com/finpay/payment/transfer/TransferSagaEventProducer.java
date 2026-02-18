package com.finpay.payment.transfer;

import com.finpay.payment.shared.config.KafkaConfig;
import com.finpay.payment.shared.outbox.OutboxService;
import com.finpay.payment.transfer.event.TransferSagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publishes transfer SAGA events via the Transactional Outbox Pattern.
 * Events are persisted to the {@code outbox_events} table within the
 * caller's database transaction and later relayed to Kafka by the
 * {@link com.finpay.payment.shared.outbox.OutboxPublisher}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferSagaEventProducer {

    private final OutboxService outboxService;

    public void sendSagaEvent(TransferSagaEvent event) {
        log.info("Saving transfer SAGA event to outbox: {} step: {} for transfer: {}",
                event.action(), event.sagaStep(), event.transferId());

        outboxService.saveEvent(
                "MoneyTransfer",
                event.transferId().toString(),
                "SAGA_" + event.sagaStep().name(),
                KafkaConfig.TRANSFER_SAGA_TOPIC,
                event.transferId().toString(),
                event
        );
    }

    public void sendNotificationEvent(TransferSagaEvent event) {
        log.info("Saving transfer notification event to outbox for transfer: {}",
                event.transferId());

        outboxService.saveEvent(
                "MoneyTransfer",
                event.transferId().toString(),
                "TRANSFER_NOTIFICATION",
                KafkaConfig.TRANSFER_NOTIFICATION_TOPIC,
                event.transferId().toString(),
                event
        );
    }
}
