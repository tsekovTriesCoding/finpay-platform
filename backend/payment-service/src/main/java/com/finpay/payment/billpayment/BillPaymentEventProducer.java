package com.finpay.payment.billpayment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.shared.config.KafkaConfig;
import com.finpay.payment.billpayment.event.BillPaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes bill-payment lifecycle events to Kafka.
 * Consumed by notification-service for user alerts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillPaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    public void sendBillPaymentEvent(BillPaymentEvent event) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            kafkaTemplate.send(
                    KafkaConfig.BILL_PAYMENT_EVENTS_TOPIC,
                    event.billPaymentId().toString(),
                    payload
            ).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent bill payment event: {} for bill: {} offset: {}",
                            event.eventType(), event.billPaymentId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send bill payment event: {} for bill: {}",
                            event.eventType(), event.billPaymentId(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize bill payment event: {}", event, e);
        }
    }
}
