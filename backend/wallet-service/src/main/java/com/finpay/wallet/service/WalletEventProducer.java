package com.finpay.wallet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.config.KafkaConfig;
import com.finpay.wallet.event.WalletResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes wallet response events to Kafka.
 * These events are consumed by payment-service to continue the saga.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class WalletEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    /**
     * Publish wallet response event after processing a command.
     * Payment-service listens for these to continue the transfer saga.
     */
    public void publishWalletResponse(WalletResponseEvent event) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            String key = event.correlationId().toString(); // Use transfer ID as key for ordering
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    KafkaConfig.WALLET_EVENTS_TOPIC,
                    key,
                    payload
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published wallet response: {} success: {} for transfer: {} offset: {}",
                            event.responseType(),
                            event.success(),
                            event.correlationId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish wallet response: {} for transfer: {}",
                            event.responseType(),
                            event.correlationId(),
                            ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize wallet response event: {}", event, e);
        }
    }
}
