package com.finpay.wallet.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.config.KafkaConfig;
import com.finpay.wallet.dto.WalletResponse;
import com.finpay.wallet.entity.Wallet;
import com.finpay.wallet.event.WalletCommandEvent;
import com.finpay.wallet.event.WalletResponseEvent;
import com.finpay.wallet.exception.InsufficientFundsException;
import com.finpay.wallet.exception.ResourceNotFoundException;
import com.finpay.wallet.exception.WalletException;
import com.finpay.wallet.service.WalletEventProducer;
import com.finpay.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for wallet command events.
 * Listens to wallet-commands topic and processes operations requested by other services.
 * Publishes response events back to wallet-events topic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletCommandConsumer {

    private final WalletService walletService;
    private final WalletEventProducer eventProducer;
    private final ObjectMapper kafkaObjectMapper;

    @KafkaListener(topics = KafkaConfig.WALLET_COMMANDS_TOPIC, groupId = "wallet-service-group")
    public void consumeWalletCommand(String message) {
        log.info("Received wallet command: {}", message);

        try {
            WalletCommandEvent command = kafkaObjectMapper.readValue(message, WalletCommandEvent.class);
            
            log.info("Processing command: {} for user: {} correlationId: {}", 
                    command.command(), command.userId(), command.correlationId());

            WalletResponseEvent response = processCommand(command);
            eventProducer.publishWalletResponse(response);
            
        } catch (Exception e) {
            log.error("Error processing wallet command: {}", e.getMessage(), e);
            // For unrecoverable errors, we could publish a generic failure event
        }
    }

    private WalletResponseEvent processCommand(WalletCommandEvent command) {
        try {
            return switch (command.command()) {
                case RESERVE_FUNDS -> handleReserveFunds(command);
                case RELEASE_FUNDS -> handleReleaseFunds(command);
                case DEDUCT_FUNDS -> handleDeductFunds(command);
                case CREDIT_FUNDS -> handleCreditFunds(command);
                case REVERSE_CREDIT -> handleReverseCredit(command);
                case REVERSE_DEDUCTION -> handleReverseDeduction(command);
            };
        } catch (InsufficientFundsException e) {
            log.warn("Insufficient funds for command {}: {}", command.command(), e.getMessage());
            return WalletResponseEvent.failure(
                    command.correlationId(),
                    command.userId(),
                    WalletResponseEvent.ResponseType.OPERATION_FAILED,
                    command.amount(),
                    command.currency(),
                    "Insufficient funds: " + e.getMessage()
            );
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found for command {}: {}", command.command(), e.getMessage());
            return WalletResponseEvent.failure(
                    command.correlationId(),
                    command.userId(),
                    WalletResponseEvent.ResponseType.OPERATION_FAILED,
                    command.amount(),
                    command.currency(),
                    "Wallet not found: " + e.getMessage()
            );
        } catch (WalletException e) {
            log.warn("Wallet error for command {}: {}", command.command(), e.getMessage());
            return WalletResponseEvent.failure(
                    command.correlationId(),
                    command.userId(),
                    WalletResponseEvent.ResponseType.OPERATION_FAILED,
                    command.amount(),
                    command.currency(),
                    "Wallet error: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error for command {}: {}", command.command(), e.getMessage(), e);
            return WalletResponseEvent.failure(
                    command.correlationId(),
                    command.userId(),
                    WalletResponseEvent.ResponseType.OPERATION_FAILED,
                    command.amount(),
                    command.currency(),
                    "Unexpected error: " + e.getMessage()
            );
        }
    }

    private WalletResponseEvent handleReserveFunds(WalletCommandEvent command) {
        log.info("Reserving {} {} for user: {}", command.amount(), command.currency(), command.userId());
        
        // Ensure wallet exists
        WalletResponse wallet = walletService.getOrCreateWallet(command.userId());
        
        // Reserve funds
        var result = walletService.reserveFunds(
                command.userId(), 
                command.amount(), 
                command.correlationId().toString()
        );
        
        return WalletResponseEvent.success(
                command.correlationId(),
                result.walletId(),
                command.userId(),
                WalletResponseEvent.ResponseType.FUNDS_RESERVED,
                command.amount(),
                result.newBalance(),
                result.newAvailableBalance(),
                command.currency()
        );
    }

    private WalletResponseEvent handleReleaseFunds(WalletCommandEvent command) {
        log.info("Releasing {} {} for user: {}", command.amount(), command.currency(), command.userId());
        
        var result = walletService.releaseReservedFunds(
                command.userId(),
                command.amount(),
                command.correlationId().toString()
        );
        
        return WalletResponseEvent.success(
                command.correlationId(),
                result.walletId(),
                command.userId(),
                WalletResponseEvent.ResponseType.FUNDS_RELEASED,
                command.amount(),
                result.newBalance(),
                result.newAvailableBalance(),
                command.currency()
        );
    }

    private WalletResponseEvent handleDeductFunds(WalletCommandEvent command) {
        log.info("Deducting {} {} from user: {}", command.amount(), command.currency(), command.userId());
        
        var result = walletService.deductFunds(
                command.userId(),
                command.amount(),
                command.correlationId().toString()
        );
        
        return WalletResponseEvent.success(
                command.correlationId(),
                result.walletId(),
                command.userId(),
                WalletResponseEvent.ResponseType.FUNDS_DEDUCTED,
                command.amount(),
                result.newBalance(),
                result.newAvailableBalance(),
                command.currency()
        );
    }

    private WalletResponseEvent handleCreditFunds(WalletCommandEvent command) {
        log.info("Crediting {} {} to user: {}", command.amount(), command.currency(), command.userId());
        
        // Ensure recipient wallet exists
        walletService.getOrCreateWallet(command.userId());
        
        var result = walletService.creditFunds(
                command.userId(),
                command.amount(),
                command.correlationId().toString()
        );
        
        return WalletResponseEvent.success(
                command.correlationId(),
                result.walletId(),
                command.userId(),
                WalletResponseEvent.ResponseType.FUNDS_CREDITED,
                command.amount(),
                result.newBalance(),
                result.newAvailableBalance(),
                command.currency()
        );
    }

    private WalletResponseEvent handleReverseCredit(WalletCommandEvent command) {
        log.info("Reversing credit of {} {} for user: {}", command.amount(), command.currency(), command.userId());
        
        var result = walletService.reverseCredit(
                command.userId(),
                command.amount(),
                command.correlationId().toString()
        );
        
        return WalletResponseEvent.success(
                command.correlationId(),
                result.walletId(),
                command.userId(),
                WalletResponseEvent.ResponseType.CREDIT_REVERSED,
                command.amount(),
                result.newBalance(),
                result.newAvailableBalance(),
                command.currency()
        );
    }

    private WalletResponseEvent handleReverseDeduction(WalletCommandEvent command) {
        log.info("Reversing deduction of {} {} for user: {}", command.amount(), command.currency(), command.userId());
        
        var result = walletService.reverseDeduction(
                command.userId(),
                command.amount(),
                command.correlationId().toString()
        );
        
        return WalletResponseEvent.success(
                command.correlationId(),
                result.walletId(),
                command.userId(),
                WalletResponseEvent.ResponseType.DEDUCTION_REVERSED,
                command.amount(),
                result.newBalance(),
                result.newAvailableBalance(),
                command.currency()
        );
    }
}
