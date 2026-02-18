package com.finpay.payment.billpayment;

import com.finpay.payment.shared.config.KafkaConfig;
import com.finpay.payment.shared.outbox.OutboxService;
import com.finpay.payment.billpayment.event.BillPaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publishes bill-payment lifecycle events via the Transactional Outbox Pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillPaymentEventProducer {

    private final OutboxService outboxService;

    public void sendBillPaymentEvent(BillPaymentEvent event) {
        log.info("Saving bill payment event to outbox: {} for bill: {}",
                event.eventType(), event.billPaymentId());

        outboxService.saveEvent(
                "BillPayment",
                event.billPaymentId().toString(),
                event.eventType().name(),
                KafkaConfig.BILL_PAYMENT_EVENTS_TOPIC,
                event.billPaymentId().toString(),
                event
        );
    }
}
