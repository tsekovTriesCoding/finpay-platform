package com.finpay.auth.config;

import com.finpay.auth.client.UserServiceApi;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

    /**
     * Default RestClient.Builder for Eureka and other non-load-balanced uses.
     * Marked @Primary so Eureka's internal HTTP client picks this one up
     * instead of the @LoadBalanced builder (which would try to resolve
     * "localhost" as a Eureka service name).
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public UserServiceApi userServiceApi(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder.baseUrl("http://user-service").build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(UserServiceApi.class);
    }
}
