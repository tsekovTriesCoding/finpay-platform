package com.finpay.wallet.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.entity.Wallet;
import com.finpay.wallet.event.UserEvent;
import com.finpay.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Kafka consumer for user events.
 * Automatically creates a wallet when a new user is registered.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "wallet-service-user-consumer")
    @Transactional
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

        // Check if wallet already exists (idempotency)
        if (walletRepository.findByUserId(event.userId()).isPresent()) {
            log.info("Wallet already exists for user: {}", event.userId());
            return;
        }

        // Create new wallet with default currency
        Wallet wallet = Wallet.builder()
                .userId(event.userId())
                .currency("USD")
                .balance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet created successfully for user: {}, walletId: {}", 
                event.userId(), savedWallet.getId());
    }

    private void handleUserDeleted(UserEvent event) {
        log.info("Deactivating wallet for deleted user: {}", event.userId());

        walletRepository.findByUserId(event.userId()).ifPresent(wallet -> {
            wallet.setStatus(Wallet.WalletStatus.CLOSED);
            walletRepository.save(wallet);
            log.info("Wallet closed for user: {}", event.userId());
        });
    }
}
