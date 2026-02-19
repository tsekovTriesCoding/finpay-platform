package com.finpay.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the FinPay Outbox + Idempotency starter.
 *
 * All properties live under the {@code finpay.outbox.*} and
 * {@code finpay.idempotency.*} namespaces and have sensible defaults.
 *
 * Example {@code application.yml}:
 *
 * finpay:
 *   outbox:
 *     batch-size: 100
 *     poll-interval-ms: 500
 *     cleanup-interval-ms: 600000
 *     retention-days: 7
 *   idempotency:
 *     cleanup-interval-ms: 3600000
 *     retention-days: 14
 */
@ConfigurationProperties(prefix = "finpay")
@Getter
@Setter
public class OutboxProperties {

    private final Outbox outbox = new Outbox();
    private final Idempotency idempotency = new Idempotency();

    @Getter
    @Setter
    public static class Outbox {
        /** Number of pending events fetched per poll cycle. */
        private int batchSize = 50;

        /** Delay between outbox poll cycles (ms). */
        private long pollIntervalMs = 500;

        /** Delay between cleanup/requeue runs (ms). */
        private long cleanupIntervalMs = 600_000;

        /** SENT events older than this many days are deleted. */
        private int retentionDays = 7;
    }

    @Getter
    @Setter
    public static class Idempotency {
        /** Delay between idempotency-table cleanup runs (ms). */
        private long cleanupIntervalMs = 3_600_000;

        /** Processed events older than this many days are deleted. */
        private int retentionDays = 14;
    }
}
