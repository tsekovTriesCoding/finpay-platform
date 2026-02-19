package com.finpay.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.outbox.config.OutboxKafkaMessageConfig;
import com.finpay.outbox.idempotency.IdempotentConsumerService;
import com.finpay.outbox.publisher.OutboxPublisher;
import com.finpay.outbox.repository.OutboxEventRepository;
import com.finpay.outbox.repository.ProcessedEventRepository;
import com.finpay.outbox.service.OutboxService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot auto-configuration for the FinPay Outbox + Idempotency starter.
 *
 * <p>Registers the following beans when the required infrastructure
 * (JPA, Kafka) is on the classpath:</p>
 * <ul>
 *   <li>{@link OutboxService} — save outbox events transactionally</li>
 *   <li>{@link OutboxPublisher} — poll &amp; publish pending events to Kafka</li>
 *   <li>{@link IdempotentConsumerService} — deduplicate incoming Kafka messages</li>
 *   <li>{@link OutboxKafkaMessageConfig} — pre-configured {@code ObjectMapper}</li>
 *   <li>{@link OutboxKafkaRetryConfig} — Kafka retry/DLT infrastructure</li>
 * </ul>
 *
 * <p>All beans are guarded by {@code @ConditionalOnMissingBean} so services
 * can override any of them if needed.</p>
 */
@AutoConfiguration
@AutoConfigurationPackage(basePackages = "com.finpay.outbox")
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
@Import(OutboxKafkaMessageConfig.class)
public class OutboxAutoConfiguration {

    // ─── Outbox (producer side) ─────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean(OutboxService.class)
    public OutboxService outboxService(OutboxEventRepository outboxEventRepository,
                                       ObjectMapper kafkaObjectMapper) {
        return new OutboxService(outboxEventRepository, kafkaObjectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(OutboxPublisher.class)
    @ConditionalOnBean(KafkaTemplate.class)
    public OutboxPublisher outboxPublisher(OutboxEventRepository outboxEventRepository,
                                           KafkaTemplate<String, String> kafkaTemplate,
                                           OutboxProperties properties) {
        return new OutboxPublisher(outboxEventRepository, kafkaTemplate, properties);
    }

    // ─── Idempotency (consumer side) ────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean(IdempotentConsumerService.class)
    public IdempotentConsumerService idempotentConsumerService(
            ProcessedEventRepository processedEventRepository,
            OutboxProperties properties) {
        return new IdempotentConsumerService(processedEventRepository, properties);
    }

}
