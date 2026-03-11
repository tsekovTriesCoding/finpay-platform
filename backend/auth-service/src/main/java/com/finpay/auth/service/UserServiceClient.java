package com.finpay.auth.service;

import com.finpay.auth.client.UserServiceApi;
import com.finpay.auth.dto.UserDto;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resilience wrapper for user-service calls.
 * Delegates to a declarative @HttpExchange client backed by a @LoadBalanced RestClient
 * that resolves "user-service" via Eureka automatically.
 *
 * Aspect order: Retry ( CircuitBreaker ( Function ) )
 * - @CircuitBreaker tracks failures and opens the circuit when threshold is exceeded.
 *   No fallback here - exceptions propagate to @Retry so retries actually happen.
 * - @Retry retries transient failures with exponential backoff. After all retries
 *   are exhausted (or for non-retryable exceptions like CallNotPermittedException),
 *   the fallback returns null so auth-service degrades to local credential data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final UserServiceApi userServiceApi;

    @CircuitBreaker(name = "user-service")
    @Retry(name = "user-service", fallbackMethod = "userServiceFallback")
    public UserDto getUserProfile(UUID userId) {
        log.debug("Fetching user profile from user-service for user: {}", userId);
        return userServiceApi.getUserProfile(userId);
    }

    private UserDto userServiceFallback(UUID userId, CallNotPermittedException e) {
        log.warn("Circuit breaker is OPEN for user-service. Failing fast for user: {}", userId);
        return null;
    }

    private UserDto userServiceFallback(UUID userId, Exception e) {
        log.warn("Fallback: user-service call failed for user {} after retries exhausted. Reason: {}",
                userId, e.getMessage());
        return null;
    }
}
