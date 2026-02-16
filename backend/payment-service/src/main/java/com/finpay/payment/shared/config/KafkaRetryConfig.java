package com.finpay.payment.shared.config;

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
 * Global Kafka retry and Dead Letter Topic (DLT) configuration for payment-service.
 *
 * Combines blocking retries for transient exceptions with non-blocking retries
 * via retry topics. Failed SAGA messages go to DLT for manual investigation.
 *
 * @see org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaRetryConfig extends RetryTopicConfigurationSupport {

    @Override
    protected void configureBlockingRetries(BlockingRetriesConfigurer blockingRetries) {
        blockingRetries
                .retryOn(
                        org.springframework.dao.TransientDataAccessException.class,
                        org.springframework.dao.QueryTimeoutException.class,
                        java.net.ConnectException.class,
                        java.io.IOException.class
                )
                .backOff(new FixedBackOff(1_000, 3));
    }

    @Override
    protected void manageNonBlockingFatalExceptions(List<Class<? extends Throwable>> nonBlockingFatalExceptions) {
        nonBlockingFatalExceptions.add(com.fasterxml.jackson.core.JsonProcessingException.class);
        nonBlockingFatalExceptions.add(IllegalArgumentException.class);
    }

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
