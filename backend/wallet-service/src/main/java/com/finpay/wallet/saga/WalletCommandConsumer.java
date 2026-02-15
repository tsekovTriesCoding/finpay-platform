package com.finpay.wallet.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.wallet.saga.event.WalletCommandEvent;
import com.finpay.wallet.saga.event.WalletResponseEvent;
import com.finpay.wallet.shared.config.KafkaConfig;
import com.finpay.wallet.shared.exception.InsufficientFundsException;
import com.finpay.wallet.shared.exception.ResourceNotFoundException;
import com.finpay.wallet.shared.exception.WalletException;
import com.finpay.wallet.wallet.WalletService;
import com.finpay.wallet.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
            return WalletResponseEvent.failure(command.correlationId(), command.userId(),
                    WalletResponseEvent.ResponseType.OPERATION_FAILED, command.amount(),
                    command.currency(), "Insufficient funds: " + e.getMessage());
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found for command {}: {}", command.command(), e.getMessage());
            return WalletResponseEvent.failure(command.correlationId(), command.userId(),
                    WalletResponseEvent.ResponseType.OPERATION_FAILED, command.amount(),
                    command.currency(), "Wallet not found: " + e.getMessage());
        } catch (WalletException e) {
            log.warn("Wallet error for command {}: {}", command.command(), e.getMessage());
            return WalletResponseEvent.failure(command.correlationId(), command.userId(),
                    WalletResponseEvent.ResponseType.OPERATION_FAILED, command.amount(),
                    command.currency(), "Wallet error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error for command {}: {}", command.command(), e.getMessage(), e);
            return WalletResponseEvent.failure(command.correlationId(), command.userId(),
                    WalletResponseEvent.ResponseType.OPERATION_FAILED, command.amount(),
                    command.currency(), "Unexpected error: " + e.getMessage());
        }
    }

    private WalletResponseEvent handleReserveFunds(WalletCommandEvent command) {
        WalletResponse wallet = walletService.getOrCreateWallet(command.userId());
        var result = walletService.reserveFunds(command.userId(), command.amount(),
                command.correlationId().toString());
        return WalletResponseEvent.success(command.correlationId(), result.walletId(), command.userId(),
                WalletResponseEvent.ResponseType.FUNDS_RESERVED, command.amount(),
                result.newBalance(), result.newAvailableBalance(), command.currency());
    }

    private WalletResponseEvent handleReleaseFunds(WalletCommandEvent command) {
        var result = walletService.releaseReservedFunds(command.userId(), command.amount(),
                command.correlationId().toString());
        return WalletResponseEvent.success(command.correlationId(), result.walletId(), command.userId(),
                WalletResponseEvent.ResponseType.FUNDS_RELEASED, command.amount(),
                result.newBalance(), result.newAvailableBalance(), command.currency());
    }

    private WalletResponseEvent handleDeductFunds(WalletCommandEvent command) {
        var result = walletService.deductFunds(command.userId(), command.amount(),
                command.correlationId().toString());
        return WalletResponseEvent.success(command.correlationId(), result.walletId(), command.userId(),
                WalletResponseEvent.ResponseType.FUNDS_DEDUCTED, command.amount(),
                result.newBalance(), result.newAvailableBalance(), command.currency());
    }

    private WalletResponseEvent handleCreditFunds(WalletCommandEvent command) {
        walletService.getOrCreateWallet(command.userId());
        var result = walletService.creditFunds(command.userId(), command.amount(),
                command.correlationId().toString());
        return WalletResponseEvent.success(command.correlationId(), result.walletId(), command.userId(),
                WalletResponseEvent.ResponseType.FUNDS_CREDITED, command.amount(),
                result.newBalance(), result.newAvailableBalance(), command.currency());
    }

    private WalletResponseEvent handleReverseCredit(WalletCommandEvent command) {
        var result = walletService.reverseCredit(command.userId(), command.amount(),
                command.correlationId().toString());
        return WalletResponseEvent.success(command.correlationId(), result.walletId(), command.userId(),
                WalletResponseEvent.ResponseType.CREDIT_REVERSED, command.amount(),
                result.newBalance(), result.newAvailableBalance(), command.currency());
    }

    private WalletResponseEvent handleReverseDeduction(WalletCommandEvent command) {
        var result = walletService.reverseDeduction(command.userId(), command.amount(),
                command.correlationId().toString());
        return WalletResponseEvent.success(command.correlationId(), result.walletId(), command.userId(),
                WalletResponseEvent.ResponseType.DEDUCTION_REVERSED, command.amount(),
                result.newBalance(), result.newAvailableBalance(), command.currency());
    }
}
