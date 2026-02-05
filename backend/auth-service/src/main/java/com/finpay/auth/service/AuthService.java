package com.finpay.auth.service;

import com.finpay.auth.client.UserServiceClient;
import com.finpay.auth.dto.*;
import com.finpay.auth.entity.RefreshToken;
import com.finpay.auth.exception.InvalidTokenException;
import com.finpay.auth.exception.UserAlreadyExistsException;
import com.finpay.auth.repository.RefreshTokenRepository;
import com.finpay.auth.security.JwtService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserServiceClient userServiceClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.email());

        // Check if user already exists
        if (userServiceClient.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        if (request.phoneNumber() != null && userServiceClient.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("User with phone number " + request.phoneNumber() + " already exists");
        }

        // Create user in user-service
        String encodedPassword = passwordEncoder.encode(request.password());
        CreateUserRequest createRequest = CreateUserRequest.fromRegisterRequest(request, encodedPassword);
        
        UserDto user = userServiceClient.createUser(createRequest);
        log.info("User registered successfully with ID: {}", user.id());

        return createAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating user with email: {}", request.email());

        UserDto user;
        try {
            user = userServiceClient.getUserByEmail(request.email());
        } catch (FeignException.NotFound e) {
            log.warn("User not found for email: {}", request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        // Verify password
        if (user.password() == null || !passwordEncoder.matches(request.password(), user.password())) {
            log.warn("Invalid password for email: {}", request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        // Check user status
        if ("SUSPENDED".equals(user.status())) {
            throw new BadCredentialsException("Account is suspended");
        }

        // Update last login time
        try {
            userServiceClient.updateLastLogin(user.id());
        } catch (Exception e) {
            log.warn("Failed to update last login time for user: {}", user.id());
        }

        log.info("User logged in successfully: {}", user.email());
        
        return createAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Refreshing token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        // Get user from user-service
        UserDto user = userServiceClient.getUserById(refreshToken.getUserId());

        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return createAuthResponse(user);
    }

    public void logout(String refreshTokenValue) {
        log.debug("Logging out user");

        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    public void logoutAll(UUID userId) {
        log.debug("Logging out all sessions for user: {}", userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    public UserDto getCurrentUser(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new InvalidTokenException("Invalid or expired token");
        }
        
        UUID userId = jwtService.extractUserIdAsUUID(token);
        return userServiceClient.getUserById(userId).withoutPassword();
    }

    private AuthResponse createAuthResponse(UserDto user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        // Save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(user.id())
                .userEmail(user.email())
                .expiryDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiration() / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        // Return user without password
        UserDto safeUser = user.withoutPassword();

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                jwtService.getAccessTokenExpiration(),
                safeUser
        );
    }
}
