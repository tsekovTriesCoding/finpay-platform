package com.finpay.payment.shared.kafka;

import com.finpay.payment.shared.event.WalletCommandEvent;
import com.finpay.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kafka producer for sending wallet command events.
 * Part of the SAGA choreography pattern.

 * Uses the Transactional Outbox Pattern: commands are persisted to the
 * {@code outbox_events} table within the caller's database transaction
 * instead of being sent directly to Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletCommandProducer {

    private static final String WALLET_COMMANDS_TOPIC = "wallet-commands";

    private final OutboxService outboxService;

    public void sendCommand(WalletCommandEvent event) {
        log.info("Saving wallet command to outbox: {} for correlationId: {}, userId: {}, amount: {}",
                event.command(), event.correlationId(), event.userId(), event.amount());

        outboxService.saveEvent(
                "WalletCommand",
                event.correlationId().toString(),
                event.command().name(),
                WALLET_COMMANDS_TOPIC,
                event.correlationId().toString(),
                event
        );
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
