package com.finpay.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.config.KafkaConfig;
import com.finpay.payment.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    public void sendPaymentEvent(PaymentEvent event) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    KafkaConfig.PAYMENT_EVENTS_TOPIC,
                    event.paymentId().toString(),
                    payload
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent payment event: {} for payment: {} with offset: {}",
                            event.eventType(),
                            event.paymentId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send payment event: {} for payment: {}",
                            event.eventType(),
                            event.paymentId(),
                            ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment event: {}", event, e);
        }
    }
}
