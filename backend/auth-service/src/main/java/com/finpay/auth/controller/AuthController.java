package com.finpay.auth.controller;

import com.finpay.auth.dto.*;
import com.finpay.auth.security.JwtService;
import com.finpay.auth.service.AuthService;
import com.finpay.auth.service.CookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieService cookieService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        cookieService.setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse.withoutTokens());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        cookieService.setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken());
        return ResponseEntity.ok(authResponse.withoutTokens());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        AuthResponse authResponse = authService.refreshToken(refreshRequest);
        cookieService.setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken());
        return ResponseEntity.ok(authResponse.withoutTokens());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        cookieService.clearAuthCookies(response);
        return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            HttpServletRequest request,
            HttpServletResponse response) {
        String token = extractAccessTokenFromCookie(request);
        if (token == null) {
            token = extractTokenFromHeader(request);
        }
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        UUID userId = jwtService.extractUserIdAsUUID(token);
        authService.logoutAll(userId);
        cookieService.clearAuthCookies(response);
        return ResponseEntity.ok(Map.of("message", "Successfully logged out from all devices"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(HttpServletRequest request) {
        String token = extractAccessTokenFromCookie(request);
        if (token == null) {
            token = extractTokenFromHeader(request);
        }
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        UserDto user = authService.getCurrentUser(token);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        String token = extractAccessTokenFromCookie(request);
        if (token == null) {
            token = extractTokenFromHeader(request);
        }
        if (token == null) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
        
        boolean isValid = jwtService.isTokenValid(token);
        
        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", jwtService.extractUserId(token),
                    "email", jwtService.extractEmail(token),
                    "role", jwtService.extractRole(token)
            ));
        }
        
        return ResponseEntity.ok(Map.of("valid", false));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running");
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        return extractCookieValue(request, CookieService.ACCESS_TOKEN_COOKIE);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        return extractCookieValue(request, CookieService.REFRESH_TOKEN_COOKIE);
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> cookieName.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
