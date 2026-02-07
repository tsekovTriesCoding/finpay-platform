package com.finpay.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.config.KafkaConfig;
import com.finpay.payment.event.MoneyRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for money request lifecycle events.
 * Publishes to the money-request-events topic consumed by notification-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoneyRequestEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    public void publishRequestEvent(MoneyRequestEvent event) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    KafkaConfig.MONEY_REQUEST_EVENTS_TOPIC,
                    event.requestId().toString(),
                    payload
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published money request event: {} for request: {} with offset: {}",
                            event.eventType(), event.requestId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish money request event: {} for request: {}",
                            event.eventType(), event.requestId(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize money request event: {}", event, e);
        }
    }
}
