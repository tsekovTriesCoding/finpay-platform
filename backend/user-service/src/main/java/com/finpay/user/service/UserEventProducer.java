package com.finpay.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.user.config.KafkaConfig;
import com.finpay.user.event.UserEvent;
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
public class UserEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    public void sendUserEvent(UserEvent event) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    KafkaConfig.USER_EVENTS_TOPIC,
                    event.userId().toString(),
                    payload
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent user event: {} for user: {} with offset: {}",
                            event.eventType(),
                            event.userId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send user event: {} for user: {}",
                            event.eventType(),
                            event.userId(),
                            ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user event: {}", event, e);
        }
    }
}
