package com.finpay.auth.service;

import com.finpay.auth.client.UserServiceApi;
import com.finpay.auth.dto.UserDto;
import com.finpay.auth.testconfig.TestcontainersConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        // Use small values for fast tests
        "resilience4j.circuitbreaker.instances.user-service.slidingWindowSize=4",
        "resilience4j.circuitbreaker.instances.user-service.minimumNumberOfCalls=2",
        "resilience4j.circuitbreaker.instances.user-service.failureRateThreshold=50",
        "resilience4j.circuitbreaker.instances.user-service.waitDurationInOpenState=1s",
        "resilience4j.circuitbreaker.instances.user-service.permittedNumberOfCallsInHalfOpenState=1",
        "resilience4j.circuitbreaker.instances.user-service.automaticTransitionFromOpenToHalfOpenEnabled=false",
        "resilience4j.retry.instances.user-service.maxAttempts=3",
        "resilience4j.retry.instances.user-service.waitDuration=100ms",
        "resilience4j.retry.instances.user-service.enableExponentialBackoff=false",
        "spring.cloud.circuitbreaker.resilience4j.disable-time-limiter=true"
})
@Import(TestcontainersConfig.class)
@DisplayName("UserServiceClient Resilience Tests")
class UserServiceClientTest {

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private UserServiceApi userServiceApi;

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        // Reset circuit breaker state before each test
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("user-service");
        cb.reset();
    }

    @Nested
    @DisplayName("Fallback Behavior")
    class FallbackBehavior {

        @Test
        @DisplayName("Should return null when user-service throws an exception")
        void shouldReturnNullWhenServiceThrows() {
            when(userServiceApi.getUserProfile(TEST_USER_ID))
                    .thenThrow(new RestClientException("Connection refused"));

            UserDto result = userServiceClient.getUserProfile(TEST_USER_ID);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return user when user-service responds successfully")
        void shouldReturnUserWhenServiceAvailable() {
            UserDto expected = new UserDto(TEST_USER_ID, "test@example.com",
                    null, "Test", "User", null, "ACTIVE", "USER",
                    null, null, null, null, null, null, null,
                    false, false, "BASIC", null, null, null);
            when(userServiceApi.getUserProfile(TEST_USER_ID)).thenReturn(expected);

            UserDto result = userServiceClient.getUserProfile(TEST_USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker States")
    class CircuitBreakerStates {

        @Test
        @DisplayName("Circuit breaker should start in CLOSED state")
        void shouldStartClosed() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("user-service");

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("Circuit breaker should open after failure threshold is reached")
        void shouldOpenAfterFailureThreshold() {
            when(userServiceApi.getUserProfile(TEST_USER_ID))
                    .thenThrow(new RestClientException("Connection refused"));

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("user-service");

            // Make enough failing calls to trip the circuit (minimumNumberOfCalls=2, threshold=50%)
            for (int i = 0; i < 3; i++) {
                userServiceClient.getUserProfile(TEST_USER_ID);
            }

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("Should fail fast with fallback when circuit is OPEN")
        void shouldFailFastWhenCircuitOpen() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("user-service");
            cb.transitionToOpenState();

            UserDto result = userServiceClient.getUserProfile(TEST_USER_ID);

            assertThat(result).isNull();
            // Metrics should show not-permitted call
            assertThat(cb.getMetrics().getNumberOfNotPermittedCalls()).isGreaterThan(0);
        }
    }
}
