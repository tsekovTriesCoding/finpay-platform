package com.finpay.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.notification.config.KafkaMessageConfig;
import com.finpay.notification.event.NotificationPreferenceEvent;
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
public class NotificationPreferenceEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    public void sendPreferenceEvent(NotificationPreferenceEvent event) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    KafkaMessageConfig.NOTIFICATION_PREFERENCE_EVENTS_TOPIC,
                    event.userId().toString(),
                    payload
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent notification preference event: {} for user: {} with offset: {}",
                            event.eventType(),
                            event.userId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send notification preference event for user: {}",
                            event.userId(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification preference event: {}", event, e);
        }
    }
}
