package com.finpay.gateway.filter;

import com.finpay.gateway.config.RateLimitProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Redis-backed sliding-window rate limiter.
 *
 * Runs before all other gateway filters.  Uses a Lua script to
 * atomically increment a per-client counter in Redis and check
 * against the configured limit.  Different rate tiers apply to
 * auth, admin, and general API endpoints.
 *
 * The client key is derived from the X-User-Id header (set by
 * AdminAuthFilter for authenticated users) or the client IP
 * for anonymous requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RateLimitFilter implements Filter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;

        // Lua script: sliding-window counter using a sorted set
        // Returns 1 if allowed, 0 if rate limit exceeded
        String lua = """
                local key = KEYS[1]
                local window = tonumber(ARGV[1])
                local limit = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
                local window_start = now - window
                redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)
                local count = redis.call('ZCARD', key)
                if count < limit then
                    redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
                    redis.call('EXPIRE', key, window)
                    return 1
                end
                return 0
                """;
        this.rateLimitScript = new DefaultRedisScript<>(lua, Long.class);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String clientKey = resolveClientKey(httpRequest);
        RateTier tier = resolveTier(path);

        String redisKey = "gateway:ratelimit:" + tier.name().toLowerCase() + ":" + clientKey;

        try {
            long nowMillis = System.currentTimeMillis();
            Long allowed = redisTemplate.execute(
                    rateLimitScript,
                    List.of(redisKey),
                    String.valueOf(tier.windowSeconds()),
                    String.valueOf(tier.maxRequests()),
                    String.valueOf(nowMillis)
            );

            if (allowed != null && allowed == 0L) {
                log.warn("Rate limit exceeded for client: {} on path: {} (tier: {})", clientKey, path, tier.name());
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.setHeader("Retry-After", String.valueOf(tier.windowSeconds()));
                httpResponse.getWriter().write(
                        "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again later.\"}");
                return;
            }
        } catch (Exception e) {
            // If Redis is down, allow the request (fail-open) to avoid blocking all traffic
            log.warn("Rate limiter unavailable, allowing request: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        // Prefer authenticated user ID
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        // Fall back to client IP
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private RateTier resolveTier(String path) {
        if (path.startsWith("/api/v1/auth/")) {
            return new RateTier("AUTH", properties.getAuthRate(), properties.getAuthWindowSeconds());
        }
        if (path.startsWith("/api/v1/admin/")) {
            return new RateTier("ADMIN", properties.getAdminRate(), properties.getAdminWindowSeconds());
        }
        return new RateTier("DEFAULT", properties.getDefaultRate(), properties.getDefaultWindowSeconds());
    }

    private record RateTier(String name, int maxRequests, int windowSeconds) {}
}
