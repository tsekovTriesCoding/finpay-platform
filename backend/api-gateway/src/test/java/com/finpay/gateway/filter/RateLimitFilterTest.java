package com.finpay.gateway.filter;

import com.finpay.gateway.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Unit Tests")
class RateLimitFilterTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Spy  private RateLimitProperties properties;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        properties.setDefaultRate(100);
        properties.setDefaultWindowSeconds(60);
        properties.setAuthRate(20);
        properties.setAuthWindowSeconds(60);
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimitingTests {

        @Test
        @DisplayName("should allow request when under rate limit")
        void shouldAllowRequestUnderLimit() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(), any(), any()))
                    .thenReturn(1L);

            rateLimitFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("should block request when rate limit exceeded")
        void shouldBlockWhenRateLimitExceeded() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(), any(), any()))
                    .thenReturn(0L);

            rateLimitFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        }

        @Test
        @DisplayName("should allow request when rate limiting is disabled")
        void shouldAllowWhenDisabled() throws Exception {
            properties.setEnabled(false);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            rateLimitFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(429);
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("should fail open when Redis is unavailable")
        void shouldFailOpenWhenRedisDown() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Redis connection refused"));

            rateLimitFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("should use user ID as key when X-User-Id header is present")
        void shouldUseUserIdForAuthenticatedRequests() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/me");
            request.addHeader("X-User-Id", "user-123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(), any(), any()))
                    .thenReturn(1L);

            rateLimitFilter.doFilter(request, response, filterChain);

            verify(redisTemplate).execute(
                    ArgumentMatchers.<RedisScript<Long>>any(),
                    eq(List.of("gateway:ratelimit:default:user:user-123")),
                    any(), any(), any()
            );
        }

        @Test
        @DisplayName("should apply auth tier rate limit for auth endpoints")
        void shouldApplyAuthTierForAuthEndpoints() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(), any(), any()))
                    .thenReturn(1L);

            rateLimitFilter.doFilter(request, response, filterChain);

            verify(redisTemplate).execute(
                    ArgumentMatchers.<RedisScript<Long>>any(),
                    eq(List.of("gateway:ratelimit:auth:ip:10.0.0.1")),
                    any(), any(), any()
            );
        }
    }
}
