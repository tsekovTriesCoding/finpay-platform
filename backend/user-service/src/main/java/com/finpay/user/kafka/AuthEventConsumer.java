package com.finpay.user.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.user.config.KafkaConfig;
import com.finpay.user.entity.User;
import com.finpay.user.event.UserEvent;
import com.finpay.user.event.UserRegisteredEvent;
import com.finpay.user.mapper.UserMapper;
import com.finpay.user.repository.UserRepository;
import com.finpay.user.service.UserEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes user registration events from auth-service.
 * Creates full user profiles when users register via auth-service.
 * Publishes USER_CREATED events for wallet-service to create wallets automatically.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventConsumer {

    private final UserRepository userRepository;
    private final ObjectMapper kafkaObjectMapper;
    private final UserEventProducer userEventProducer;
    private final UserMapper userMapper;

    @KafkaListener(topics = KafkaConfig.AUTH_EVENTS_TOPIC, groupId = "user-service-group")
    @Transactional
    public void handleAuthEvent(String message) {
        log.info("Received auth event: {}", message);
        
        try {
            UserRegisteredEvent event = kafkaObjectMapper.readValue(message, UserRegisteredEvent.class);
            handleUserRegistered(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize auth event: {}", message, e);
        }
    }

    private void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Processing user registration event for user: {}", event.userId());
        
        // Check if user already exists (idempotency)
        if (userRepository.existsById(event.userId())) {
            log.info("User {} already exists, skipping creation", event.userId());
            return;
        }
        
        if (userRepository.existsByEmail(event.email())) {
            log.warn("User with email {} already exists with different ID, skipping", event.email());
            return;
        }
        
        // Determine auth provider
        User.AuthProvider authProvider = User.AuthProvider.LOCAL;
        if (event.isOAuthUser()) {
            authProvider = switch (event.oauthProvider().toUpperCase()) {
                case "GOOGLE" -> User.AuthProvider.GOOGLE;
                case "GITHUB" -> User.AuthProvider.GITHUB;
                default -> User.AuthProvider.LOCAL;
            };
        }
        
        // Create user profile
        User user = User.builder()
                .id(event.userId())
                .email(event.email())
                .password("")  // Auth-service owns credentials, we store empty placeholder
                .firstName(event.firstName())
                .lastName(event.lastName())
                .phoneNumber(event.phoneNumber())
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .authProvider(authProvider)
                .providerId(event.oauthProviderId())
                .profileImageUrl(event.profileImageUrl())
                .emailVerified(event.isOAuthUser())  // OAuth users have verified email
                .phoneVerified(false)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Created user profile for: {} with ID: {}", savedUser.getEmail(), savedUser.getId());
        
        // Publish USER_CREATED event for wallet-service to create wallet automatically
        UserEvent userEvent = userMapper.toEvent(savedUser, UserEvent.EventType.USER_CREATED);
        userEventProducer.sendUserEvent(userEvent);
        log.info("Published USER_CREATED event for user: {}", savedUser.getId());
    }
}
