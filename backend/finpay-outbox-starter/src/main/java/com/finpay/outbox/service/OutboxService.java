package com.finpay.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.outbox.entity.OutboxEvent;
import com.finpay.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves {@link OutboxEvent} entries inside the current database transaction.
 *
 * Business services call {@code saveEvent()} within their transactional
 * methods; the background {@link com.finpay.outbox.publisher.OutboxPublisher}
 * then publishes them to Kafka asynchronously.
 */
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper kafkaObjectMapper;

    /**
     * Serialise {@code payload} to JSON and persist an outbox event.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveEvent(String aggregateType, String aggregateId,
                          String eventType, String topic,
                          String eventKey, Object payload) {
        try {
            String json = kafkaObjectMapper.writeValueAsString(payload);
            saveEvent(aggregateType, aggregateId, eventType, topic, eventKey, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise outbox payload for {}: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Outbox serialisation failed", e);
        }
    }

    /**
     * Persist an outbox event with a pre-serialised JSON payload.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveEvent(String aggregateType, String aggregateId,
                          String eventType, String topic,
                          String eventKey, String jsonPayload) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic(topic)
                .eventKey(eventKey)
                .payload(jsonPayload)
                .build();

        outboxEventRepository.save(event);
        log.debug("Outbox event saved: type={}, aggregateId={}, topic={}",
                eventType, aggregateId, topic);
    }
}
