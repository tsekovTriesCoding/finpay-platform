package com.finpay.wallet.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.saga.event.WalletResponseEvent;
import com.finpay.wallet.shared.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    public void publishWalletResponse(WalletResponseEvent event) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            String key = event.correlationId().toString();

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    KafkaConfig.WALLET_EVENTS_TOPIC, key, payload);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published wallet response: {} success: {} for transfer: {} offset: {}",
                            event.responseType(), event.success(), event.correlationId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish wallet response: {} for transfer: {}",
                            event.responseType(), event.correlationId(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize wallet response event: {}", event, e);
        }
    }
}
