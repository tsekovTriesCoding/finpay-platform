package com.finpay.auth.service;

import com.finpay.auth.dto.*;
import com.finpay.auth.entity.RefreshToken;
import com.finpay.auth.entity.UserCredential;
import com.finpay.auth.event.UserRegisteredEvent;
import com.finpay.auth.exception.InvalidTokenException;
import com.finpay.auth.exception.UserAlreadyExistsException;
import com.finpay.auth.kafka.AuthEventProducer;
import com.finpay.auth.repository.RefreshTokenRepository;
import com.finpay.auth.repository.UserCredentialRepository;
import com.finpay.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication service using local credential storage.
 * User profile data is synced to user-service via Kafka events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthEventProducer authEventProducer;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.email());

        // Check if user already exists
        if (credentialRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        if (request.phoneNumber() != null && credentialRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("User with phone number " + request.phoneNumber() + " already exists");
        }

        // Create local credential
        String encodedPassword = passwordEncoder.encode(request.password());
        UserCredential credential = UserCredential.builder()
                .email(request.email())
                .passwordHash(encodedPassword)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .enabled(true)
                .accountLocked(false)
                .emailVerified(false)
                .build();

        UserCredential savedCredential = credentialRepository.save(credential);
        log.info("User registered successfully with ID: {}", savedCredential.getId());

        // Publish event for user-service to create full profile
        UserRegisteredEvent event = UserRegisteredEvent.create(
                savedCredential.getId(),
                savedCredential.getEmail(),
                savedCredential.getFirstName(),
                savedCredential.getLastName(),
                savedCredential.getPhoneNumber()
        );
        authEventProducer.publishUserRegistered(event);

        return createAuthResponse(savedCredential);
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating user with email: {}", request.email());

        UserCredential credential = credentialRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", request.email());
                    return new BadCredentialsException("Invalid email or password");
                });

        // Verify password
        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            log.warn("Invalid password for email: {}", request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        // Check user status
        if (!credential.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        if (credential.isAccountLocked()) {
            throw new BadCredentialsException("Account is locked");
        }

        // Update last login time
        credential.setLastLoginAt(LocalDateTime.now());
        credentialRepository.save(credential);

        log.info("User logged in successfully: {}", credential.getEmail());
        
        return createAuthResponse(credential);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Refreshing token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        // Get credential
        UserCredential credential = credentialRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return createAuthResponse(credential);
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
        UserCredential credential = credentialRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));
        
        return toUserDto(credential);
    }

    private AuthResponse createAuthResponse(UserCredential credential) {
        UserDto userDto = toUserDto(credential);
        
        String accessToken = jwtService.generateAccessToken(userDto);
        String refreshTokenValue = jwtService.generateRefreshToken(userDto);

        // Save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(credential.getId())
                .userEmail(credential.getEmail())
                .expiryDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiration() / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                jwtService.getAccessTokenExpiration(),
                userDto
        );
    }

    private UserDto toUserDto(UserCredential credential) {
        return new UserDto(
                credential.getId(),
                credential.getEmail(),
                null,  // Never expose password
                credential.getFirstName(),
                credential.getLastName(),
                credential.getPhoneNumber(),
                credential.isEnabled() ? "ACTIVE" : "INACTIVE",
                "USER",
                credential.getOauthProvider(),
                credential.getOauthProviderId(),
                credential.getProfileImageUrl(),
                null, null, null, null,  // Address fields - not stored in auth
                credential.isEmailVerified(),
                false,  // Phone verified - not tracked here
                credential.getCreatedAt(),
                credential.getUpdatedAt(),
                credential.getLastLoginAt()
        );
    }
}
