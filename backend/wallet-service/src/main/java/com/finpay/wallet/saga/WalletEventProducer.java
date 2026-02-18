package com.finpay.wallet.saga;

import com.finpay.wallet.saga.event.WalletResponseEvent;
import com.finpay.wallet.shared.config.KafkaConfig;
import com.finpay.wallet.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Publishes wallet response events via the Transactional Outbox Pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletEventProducer {

    private final OutboxService outboxService;

    public void publishWalletResponse(WalletResponseEvent event) {
        log.info("Saving wallet response to outbox: {} success: {} for transfer: {}",
                event.responseType(), event.success(), event.correlationId());

        outboxService.saveEvent(
                "Wallet",
                event.correlationId().toString(),
                event.responseType().name(),
                KafkaConfig.WALLET_EVENTS_TOPIC,
                event.correlationId().toString(),
                event
        );
    }
}
