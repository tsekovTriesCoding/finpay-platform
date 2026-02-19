package com.finpay.notification.shared.config;

import com.finpay.outbox.config.OutboxKafkaRetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.List;

/**
 * Notification-service Kafka retry configuration.
 *
 * <p>Extends the shared {@link OutboxKafkaRetryConfig} from
 * finpay-outbox-spring-boot-starter and adds {@code MailSendException}
 * to the blocking-retryable exceptions so transient mail-server failures
 * are retried in-line before falling through to retry topics.</p>
 *
 * <p>Also marks the {@code kafkaRetryTaskScheduler} as {@code @Primary}
 * to disambiguate from the WebSocket {@code messageBrokerTaskScheduler}.</p>
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaRetryConfig extends OutboxKafkaRetryConfig {

    @Override
    protected List<Class<? extends Exception>> additionalBlockingRetryExceptions() {
        return List.of(org.springframework.mail.MailSendException.class);
    }

    @Bean
    @Primary
    @Override
    public TaskScheduler kafkaRetryTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("kafka-retry-");
        return scheduler;
    }
}
