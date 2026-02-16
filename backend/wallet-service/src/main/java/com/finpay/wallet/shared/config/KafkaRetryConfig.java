package com.finpay.wallet.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

/**
 * Global Kafka retry and Dead Letter Topic (DLT) configuration for wallet-service.
 *
 * Uses the Spring Kafka non-blocking retry infrastructure with combined blocking
 * and non-blocking retries:
 * - Blocking retries: 3 attempts with 1s interval for transient exceptions
 *   (e.g., database connectivity issues) - keeps the consumer thread occupied but
 *   handles fast-recovering failures without topic overhead.
 * - Non-blocking retries: Configured per-listener via @RetryableTopic
 *   with exponential backoff - creates retry topics, freeing the consumer to
 *   process other messages.
 * - Dead Letter Topic (DLT): After all retries are exhausted, messages are
 *   published to <topic>-dlt for later analysis and reprocessing.
 *
 * @see org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaRetryConfig extends RetryTopicConfigurationSupport {

    /**
     * Configure blocking retries for transient exceptions that are likely to recover
     * within a few seconds (e.g., temporary DB unavailability, transient network issues).
     * These retries happen on the same consumer thread before the non-blocking retry
     * mechanism kicks in.
     */
    @Override
    protected void configureBlockingRetries(BlockingRetriesConfigurer blockingRetries) {
        blockingRetries
                .retryOn(
                        org.springframework.dao.TransientDataAccessException.class,
                        org.springframework.dao.QueryTimeoutException.class,
                        java.net.ConnectException.class,
                        java.io.IOException.class
                )
                .backOff(new FixedBackOff(1_000, 3)); // 3 attempts, 1s apart
    }

    /**
     * Configure exceptions that should skip non-blocking retries entirely and go
     * straight to the DLT. These are "fatal" exceptions where retrying is pointless.
     */
    @Override
    protected void manageNonBlockingFatalExceptions(List<Class<? extends Throwable>> nonBlockingFatalExceptions) {
        nonBlockingFatalExceptions.add(com.fasterxml.jackson.core.JsonProcessingException.class);
        nonBlockingFatalExceptions.add(IllegalArgumentException.class);
    }

    /**
     * Customize the error handler to not seek after error — uses the newer mechanism
     * where records are retained in memory and resubmitted (avoids re-fetching).
     */
    @Override
    protected void configureCustomizers(CustomizersConfigurer customizersConfigurer) {
        customizersConfigurer.customizeErrorHandler(eh -> eh.setSeekAfterError(false));
    }

    @Bean
    public TaskScheduler kafkaRetryTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("kafka-retry-");
        return scheduler;
    }
}
