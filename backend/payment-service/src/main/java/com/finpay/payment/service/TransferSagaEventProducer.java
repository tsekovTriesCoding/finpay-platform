package com.finpay.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.config.KafkaConfig;
import com.finpay.payment.event.TransferSagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferSagaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    public void sendSagaEvent(TransferSagaEvent event) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    KafkaConfig.TRANSFER_SAGA_TOPIC,
                    event.transferId().toString(),
                    payload
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent transfer SAGA event: {} step: {} for transfer: {} with offset: {}",
                            event.action(),
                            event.sagaStep(),
                            event.transferId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send transfer SAGA event: {} for transfer: {}",
                            event.sagaStep(),
                            event.transferId(),
                            ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transfer SAGA event: {}", event, e);
        }
    }

    public void sendNotificationEvent(TransferSagaEvent event) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    KafkaConfig.TRANSFER_NOTIFICATION_TOPIC,
                    event.transferId().toString(),
                    payload
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent transfer notification event for transfer: {} with offset: {}",
                            event.transferId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send transfer notification event for transfer: {}",
                            event.transferId(),
                            ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transfer notification event: {}", event, e);
        }
    }
}
