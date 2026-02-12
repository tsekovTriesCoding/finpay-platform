package com.finpay.payment.shared.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.shared.event.WalletCommandEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kafka producer for sending wallet command events.
 * Part of the SAGA choreography pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletCommandProducer {

    private static final String WALLET_COMMANDS_TOPIC = "wallet-commands";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    public void sendCommand(WalletCommandEvent event) {
        log.info("Sending wallet command: {} for correlationId: {}, userId: {}, amount: {}",
                event.command(), event.correlationId(), event.userId(), event.amount());

        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            kafkaTemplate.send(WALLET_COMMANDS_TOPIC, event.correlationId().toString(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send wallet command event: {}", event.eventId(), ex);
                        } else {
                            log.debug("Wallet command sent successfully: {}", event.eventId());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize wallet command event: {}", event, e);
        }
    }

    public void reserveFunds(UUID transferId, UUID userId, BigDecimal amount, String currency, String description) {
        WalletCommandEvent event = WalletCommandEvent.create(
                transferId, userId, WalletCommandEvent.CommandType.RESERVE_FUNDS,
                amount, currency, description
        );
        sendCommand(event);
    }

    public void releaseFunds(UUID transferId, UUID userId, BigDecimal amount, String currency, String description) {
        WalletCommandEvent event = WalletCommandEvent.create(
                transferId, userId, WalletCommandEvent.CommandType.RELEASE_FUNDS,
                amount, currency, description
        );
        sendCommand(event);
    }

    public void deductFunds(UUID transferId, UUID userId, BigDecimal amount, String currency, String description) {
        WalletCommandEvent event = WalletCommandEvent.create(
                transferId, userId, WalletCommandEvent.CommandType.DEDUCT_FUNDS,
                amount, currency, description
        );
        sendCommand(event);
    }

    public void creditFunds(UUID transferId, UUID userId, BigDecimal amount, String currency, String description) {
        WalletCommandEvent event = WalletCommandEvent.create(
                transferId, userId, WalletCommandEvent.CommandType.CREDIT_FUNDS,
                amount, currency, description
        );
        sendCommand(event);
    }

    public void reverseCredit(UUID transferId, UUID userId, BigDecimal amount, String currency, String description) {
        WalletCommandEvent event = WalletCommandEvent.create(
                transferId, userId, WalletCommandEvent.CommandType.REVERSE_CREDIT,
                amount, currency, description
        );
        sendCommand(event);
    }

    public void reverseDeduction(UUID transferId, UUID userId, BigDecimal amount, String currency, String description) {
        WalletCommandEvent event = WalletCommandEvent.create(
                transferId, userId, WalletCommandEvent.CommandType.REVERSE_DEDUCTION,
                amount, currency, description
        );
        sendCommand(event);
    }
}
