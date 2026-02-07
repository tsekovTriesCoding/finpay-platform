package com.finpay.wallet.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.event.UserEvent;
import com.finpay.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for user events.
 * Automatically creates a wallet when a new user is registered.
 * Delegates all wallet operations to WalletService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final WalletService walletService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "wallet-service-user-consumer")
    public void handleUserEvent(String message) {
        try {
            UserEvent event = objectMapper.readValue(message, UserEvent.class);
            log.info("Received user event: {} for userId: {}", event.eventType(), event.userId());

            switch (event.eventType()) {
                case USER_CREATED -> handleUserCreated(event);
                case USER_DELETED -> handleUserDeleted(event);
                default -> log.debug("Ignoring user event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process user event: {}", message, e);
        }
    }

    private void handleUserCreated(UserEvent event) {
        log.info("Creating wallet for new user: {} ({})", event.userId(), event.email());

        // Use WalletService to get or create wallet (ensures consistent initial balance and transaction recording)
        // The getOrCreateWallet method is idempotent - it returns existing wallet if one already exists
        walletService.getOrCreateWallet(event.userId());
        log.info("Wallet created/verified for user: {}", event.userId());
    }

    private void handleUserDeleted(UserEvent event) {
        log.info("Deactivating wallet for deleted user: {}", event.userId());
        walletService.closeWalletForUser(event.userId());
    }
}
