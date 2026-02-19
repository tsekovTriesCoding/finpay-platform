package com.finpay.outbox.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Provides a pre-configured {@link ObjectMapper} suitable for Kafka
 * message serialisation (Java-8 date/time support, lenient unknown
 * properties).
 *
 * The bean is {@code @Primary} so it is picked up by default, but
 * any application-level {@code ObjectMapper} bean named
 * {@code kafkaObjectMapper} will take precedence thanks to
 * {@code @ConditionalOnMissingBean}.
 */
@Configuration
public class OutboxKafkaMessageConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "kafkaObjectMapper")
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
