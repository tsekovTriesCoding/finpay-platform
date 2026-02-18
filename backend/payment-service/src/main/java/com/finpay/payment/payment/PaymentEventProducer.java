package com.finpay.payment.payment;

import com.finpay.payment.shared.config.KafkaConfig;
import com.finpay.payment.shared.outbox.OutboxService;
import com.finpay.payment.payment.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publishes payment events via the Transactional Outbox Pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final OutboxService outboxService;

    public void sendPaymentEvent(PaymentEvent event) {
        log.info("Saving payment event to outbox: {} for payment: {}",
                event.eventType(), event.paymentId());

        outboxService.saveEvent(
                "Payment",
                event.paymentId().toString(),
                event.eventType().name(),
                KafkaConfig.PAYMENT_EVENTS_TOPIC,
                event.paymentId().toString(),
                event
        );
    }
}
