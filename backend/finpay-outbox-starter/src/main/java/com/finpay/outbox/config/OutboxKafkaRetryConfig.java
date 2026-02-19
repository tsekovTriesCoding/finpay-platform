package com.finpay.outbox.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

/**
 * Base Kafka retry / DLT configuration shared across all FinPay services.
 *
 * <p>Combines blocking retries for transient infrastructure exceptions with
 * non-blocking retries via retry topics.  Fatal serialisation errors skip
 * retries and go straight to the DLT.</p>
 *
 * <p>Registered as an {@code @AutoConfiguration} so that Spring processes
 * the internal {@code @Bean} methods of {@link RetryTopicConfigurationSupport}.
 * Creating this class as a plain {@code @Bean} would skip those methods and
 * silently prevent {@code @RetryableTopic} listeners from starting.</p>
 *
 * <h3>Extensibility</h3>
 * <p>If a service needs additional blocking-retryable exceptions (e.g.
 * {@code MailSendException} in notification-service) it can subclass this
 * class, override
 * {@link #additionalBlockingRetryExceptions()}, and declare the subclass
 * as a {@code @Configuration} bean.  The auto-configuration will back off
 * because of {@code @ConditionalOnMissingBean}.</p>
 */
@AutoConfiguration
@ConditionalOnMissingBean(RetryTopicConfigurationSupport.class)
@EnableKafka
@Slf4j
public class OutboxKafkaRetryConfig extends RetryTopicConfigurationSupport {

    @Override
    protected void configureBlockingRetries(BlockingRetriesConfigurer blockingRetries) {
        blockingRetries
                .retryOn(blockingRetryExceptions())
                .backOff(new FixedBackOff(1_000, 3));
    }

    /**
     * Returns the set of exception classes eligible for blocking retries.
     * Override {@link #additionalBlockingRetryExceptions()} to add
     * service-specific exceptions without re-declaring the common ones.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Exception>[] blockingRetryExceptions() {
        List<Class<? extends Exception>> exceptions = new java.util.ArrayList<>(List.of(
                org.springframework.dao.TransientDataAccessException.class,
                org.springframework.dao.QueryTimeoutException.class,
                java.net.ConnectException.class,
                java.io.IOException.class
        ));
        List<Class<? extends Exception>> extras = additionalBlockingRetryExceptions();
        if (extras != null) {
            exceptions.addAll(extras);
        }
        return exceptions.toArray(new Class[0]);
    }

    /**
     * Override this method in service-specific subclasses to add additional
     * exception classes that should trigger blocking retries.
     *
     * @return additional exception classes (may be empty, must not be null)
     */
    protected List<Class<? extends Exception>> additionalBlockingRetryExceptions() {
        return List.of();
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
    @ConditionalOnMissingBean(name = "kafkaRetryTaskScheduler")
    public TaskScheduler kafkaRetryTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("kafka-retry-");
        return scheduler;
    }
}
