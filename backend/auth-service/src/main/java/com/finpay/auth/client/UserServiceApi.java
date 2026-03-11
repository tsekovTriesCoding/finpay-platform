package com.finpay.auth.client;

import com.finpay.auth.dto.UserDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

/**
 * Declarative HTTP client for user-service.
 * Backed by a @LoadBalanced RestClient that resolves "user-service" via Eureka.
 */
@HttpExchange("/api/v1/users")
public interface UserServiceApi {

    @GetExchange("/{userId}")
    UserDto getUserProfile(@PathVariable UUID userId);
}
