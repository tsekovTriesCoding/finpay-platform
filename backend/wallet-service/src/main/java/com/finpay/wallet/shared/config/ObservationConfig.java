package com.finpay.wallet.shared.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables @Observed annotation support for Micrometer Tracing.
 * Methods annotated with @Observed will automatically create spans
 * and metrics, visible in Zipkin and Prometheus/Grafana.
 */
@Configuration(proxyBeanMethods = false)
public class ObservationConfig {

    @Bean
    ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }
}
