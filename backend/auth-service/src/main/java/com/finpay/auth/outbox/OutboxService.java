package com.finpay.auth.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves outbox events inside the current database transaction.

 * Callers must invoke {@link #saveEvent} within an existing {@code @Transactional}
 * method so that the outbox row is committed atomically with the business data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper kafkaObjectMapper;

    /**
     * Persist an outbox event in the same transaction as the caller.
     *
     * @param aggregateType logical name of the aggregate (e.g. "UserCredential")
     * @param aggregateId   business key of the aggregate instance
     * @param eventType     event discriminator (e.g. "USER_REGISTERED")
     * @param topic         Kafka topic to publish to
     * @param eventKey      Kafka message key (for partitioning)
     * @param payload       the event object - will be serialised to JSON
     */
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
