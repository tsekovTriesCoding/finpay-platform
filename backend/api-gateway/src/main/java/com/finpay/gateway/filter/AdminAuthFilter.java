package com.finpay.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Gateway filter that:
 * 1. Parses JWT claims and forwards X-User-Id, X-User-Role, X-User-Email headers to downstream services.
 * 2. Enforces ADMIN role for all /api/v1/admin/** routes.
 *
 * Runs after JwtCookieFilter (which adds the Authorization header from cookies).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // After JwtCookieFilter (HIGHEST_PRECEDENCE + 1)
@Slf4j
public class AdminAuthFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin/";

    private final SecretKey signingKey;

    public AdminAuthFilter(@Value("${jwt.secret:default-secret-key-for-development-only-change-in-production}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestPath = httpRequest.getRequestURI();

        // Extract JWT from Authorization header (added by JwtCookieFilter)
        String authHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);
        Claims claims = null;

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception e) {
                log.debug("Failed to parse JWT: {}", e.getMessage());
            }
        }

        // Enforce ADMIN role for admin paths
        if (requestPath.startsWith(ADMIN_PATH_PREFIX)) {
            if (claims == null) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("""
                        {"status":401,"error":"Unauthorized","message":"Authentication required for admin endpoints"}""");
                return;
            }

            String role = claims.get("role", String.class);
            if (!"ADMIN".equals(role)) {
                log.warn("Non-admin user {} attempted to access admin endpoint: {}", claims.getSubject(), requestPath);
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("""
                        {"status":403,"error":"Forbidden","message":"Admin role required"}""");
                return;
            }
        }

        // Forward user claims as headers to downstream services
        if (claims != null) {
            HttpServletRequestWrapper wrappedRequest = new ClaimsHeaderWrapper(httpRequest, claims);
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Wraps request to add X-User-Id, X-User-Role, X-User-Email headers from JWT claims.
     */
    private static class ClaimsHeaderWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> extraHeaders = new HashMap<>();

        public ClaimsHeaderWrapper(HttpServletRequest request, Claims claims) {
            super(request);
            if (claims.getSubject() != null) {
                extraHeaders.put("X-User-Id", claims.getSubject());
            }
            if (claims.get("role", String.class) != null) {
                extraHeaders.put("X-User-Role", claims.get("role", String.class));
            }
            if (claims.get("email", String.class) != null) {
                extraHeaders.put("X-User-Email", claims.get("email", String.class));
            }
        }

        @Override
        public String getHeader(String name) {
            String customValue = extraHeaders.get(name);
            return customValue != null ? customValue : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (extraHeaders.containsKey(name)) {
                return Collections.enumeration(Collections.singletonList(extraHeaders.get(name)));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> headerNames = new HashSet<>(extraHeaders.keySet());
            Enumeration<String> originalNames = super.getHeaderNames();
            while (originalNames.hasMoreElements()) {
                headerNames.add(originalNames.nextElement());
            }
            return Collections.enumeration(headerNames);
        }
    }
}
