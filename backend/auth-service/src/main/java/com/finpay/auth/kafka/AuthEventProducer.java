package com.finpay.auth.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.auth.config.KafkaConfig;
import com.finpay.auth.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for auth events.
 * Publishes user registration events for user-service to consume.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishUserRegistered(UserRegisteredEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.AUTH_EVENTS_TOPIC, event.userId().toString(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Published UserRegisteredEvent for user: {} with offset: {}",
                                    event.email(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish UserRegisteredEvent for user: {}",
                                    event.email(), ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize UserRegisteredEvent: {}", event, e);
        }
    }
}
