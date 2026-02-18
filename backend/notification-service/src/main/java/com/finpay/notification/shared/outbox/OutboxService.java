package com.finpay.notification.shared.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves outbox events inside the current database transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper kafkaObjectMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveEvent(String aggregateType, String aggregateId,
                          String eventType, String topic,
                          String eventKey, Object payload) {
        try {
            String json = kafkaObjectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .topic(topic)
                    .eventKey(eventKey)
                    .payload(json)
                    .build();

            outboxEventRepository.save(event);
            log.debug("Outbox event saved: type={}, aggregateId={}, topic={}",
                    eventType, aggregateId, topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise outbox payload for {}: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Outbox serialisation failed", e);
        }
    }
}
