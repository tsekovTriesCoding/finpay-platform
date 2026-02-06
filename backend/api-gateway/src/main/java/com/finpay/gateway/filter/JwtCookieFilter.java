package com.finpay.gateway.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Filter that extracts JWT from HTTP-only cookies and adds it to the Authorization header.
 * This allows the gateway to forward authenticated requests to downstream services
 * while keeping tokens secure in HTTP-only cookies.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class JwtCookieFilter implements Filter {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Check if Authorization header is already present
        String existingAuth = httpRequest.getHeader(AUTHORIZATION_HEADER);
        if (existingAuth != null && existingAuth.startsWith(BEARER_PREFIX)) {
            // Authorization header already present, proceed without modification
            chain.doFilter(request, response);
            return;
        }
        
        // Try to extract JWT from cookie
        String jwtToken = extractAccessTokenFromCookie(httpRequest);
        
        if (jwtToken != null) {
            // Wrap the request to add the Authorization header
            HttpServletRequestWrapper wrappedRequest = new AuthorizationHeaderWrapper(httpRequest, jwtToken);
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> ACCESS_TOKEN_COOKIE.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .filter(value -> value != null && !value.isEmpty())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Wrapper that adds the Authorization header with the JWT token
     */
    private static class AuthorizationHeaderWrapper extends HttpServletRequestWrapper {
        private final String jwtToken;
        private final Map<String, String> customHeaders = new HashMap<>();

        public AuthorizationHeaderWrapper(HttpServletRequest request, String jwtToken) {
            super(request);
            this.jwtToken = jwtToken;
            this.customHeaders.put(AUTHORIZATION_HEADER, BEARER_PREFIX + jwtToken);
        }

        @Override
        public String getHeader(String name) {
            String customValue = customHeaders.get(name);
            if (customValue != null) {
                return customValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (customHeaders.containsKey(name)) {
                return Collections.enumeration(Collections.singletonList(customHeaders.get(name)));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> headerNames = new HashSet<>(customHeaders.keySet());
            Enumeration<String> originalNames = super.getHeaderNames();
            while (originalNames.hasMoreElements()) {
                headerNames.add(originalNames.nextElement());
            }
            return Collections.enumeration(headerNames);
        }
    }
}
