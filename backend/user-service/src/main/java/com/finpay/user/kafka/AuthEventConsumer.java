package com.finpay.user.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.user.config.KafkaConfig;
import com.finpay.user.entity.User;
import com.finpay.user.event.PlanUpgradedEvent;
import com.finpay.user.event.UserEvent;
import com.finpay.user.event.UserRegisteredEvent;
import com.finpay.outbox.idempotency.IdempotentConsumerService;
import com.finpay.user.mapper.UserMapper;
import com.finpay.user.repository.UserRepository;
import com.finpay.user.service.UserEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes user registration events from auth-service.
 * Creates full user profiles when users register via auth-service.
 * Publishes USER_CREATED events for wallet-service to create wallets automatically.
 *
 * Configured with non-blocking retries and DLT:
 * - 4 attempts with exponential backoff (1s, 2s, 4s)
 * - Failed messages go to auth-events-dlt
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventConsumer {

    private final UserRepository userRepository;
    private final ObjectMapper kafkaObjectMapper;
    private final UserEventProducer userEventProducer;
    private final UserMapper userMapper;
    private final IdempotentConsumerService idempotentConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = KafkaConfig.AUTH_EVENTS_TOPIC, groupId = "user-service-group")
    @Transactional
    public void handleAuthEvent(String message,
                                @Header(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                @Header(value = "X-Event-Type", required = false) String eventType) throws Exception {
        if (idempotentConsumer.isDuplicate(idempotencyKey)) {
            log.info("Duplicate auth event detected, skipping: idempotencyKey={}", idempotencyKey);
            return;
        }

        log.info("Received auth event: type={}, payload={}", eventType, message);

        switch (eventType != null ? eventType : "USER_REGISTERED") {
            case "USER_REGISTERED" -> {
                UserRegisteredEvent event = kafkaObjectMapper.readValue(message, UserRegisteredEvent.class);
                handleUserRegistered(event);
            }
            case "PLAN_UPGRADED" -> {
                PlanUpgradedEvent event = kafkaObjectMapper.readValue(message, PlanUpgradedEvent.class);
                handlePlanUpgraded(event);
            }
            default -> log.warn("Unknown auth event type: {}", eventType);
        }

        idempotentConsumer.markProcessed(idempotencyKey, "auth-event-consumer");
    }

    /**
     * Dead Letter Topic handler for auth events that failed all retry attempts.
     */
    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        log.error("DLT: Failed to process auth event after all retries. Topic: {}, Key: {}, Value: {}, Error: {}",
                topic, record.key(), record.value(), errorMessage);
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
        
        // Determine account plan
        User.AccountPlan accountPlan = User.AccountPlan.STARTER;
        if (event.plan() != null) {
            try {
                accountPlan = User.AccountPlan.valueOf(event.plan());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown plan '{}' for user {}, defaulting to STARTER", event.plan(), event.userId());
            }
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
                .plan(accountPlan)
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

    private void handlePlanUpgraded(PlanUpgradedEvent event) {
        log.info("Processing plan upgrade event for user: {} ({} -> {})",
                event.userId(), event.previousPlan(), event.newPlan());

        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) {
            log.warn("User {} not found for plan upgrade, skipping", event.userId());
            return;
        }

        User.AccountPlan newPlan;
        try {
            newPlan = User.AccountPlan.valueOf(event.newPlan());
        } catch (IllegalArgumentException e) {
            log.error("Unknown plan '{}' in plan upgrade event for user {}", event.newPlan(), event.userId());
            return;
        }

        user.setPlan(newPlan);
        User updatedUser = userRepository.save(user);
        log.info("Updated user {} plan to {}", updatedUser.getId(), newPlan);

        // Publish PLAN_UPGRADED event for wallet-service and notification-service
        UserEvent userEvent = userMapper.toEvent(updatedUser, UserEvent.EventType.PLAN_UPGRADED);
        userEventProducer.sendUserEvent(userEvent);
        log.info("Published PLAN_UPGRADED event for user: {}", updatedUser.getId());
    }
}
