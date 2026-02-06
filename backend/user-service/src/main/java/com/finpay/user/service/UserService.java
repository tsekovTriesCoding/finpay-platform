package com.finpay.user.service;

import com.finpay.user.dto.CreateUserRequest;
import com.finpay.user.dto.UserRequest;
import com.finpay.user.dto.UserResponse;
import com.finpay.user.dto.UserSearchResponse;
import com.finpay.user.entity.User;
import com.finpay.user.event.UserEvent;
import com.finpay.user.exception.ResourceNotFoundException;
import com.finpay.user.exception.UserAlreadyExistsException;
import com.finpay.user.mapper.UserMapper;
import com.finpay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserEventProducer userEventProducer;
    private final UserMapper userMapper;

    public UserResponse createUser(UserRequest request) {
        log.info("Creating new user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        if (request.phoneNumber() != null && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("User with phone number " + request.phoneNumber() + " already exists");
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        // Publish user created event
        publishUserEvent(savedUser, UserEvent.EventType.USER_CREATED);

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        log.debug("Fetching user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * Search users by name or email for money transfer recipient selection.
     * Excludes the current user from results.
     */
    @Transactional(readOnly = true)
    public Page<UserSearchResponse> searchUsers(String searchTerm, UUID excludeUserId, int page, int size) {
        log.debug("Searching users with term: '{}', excluding user: {}", searchTerm, excludeUserId);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Page.empty();
        }
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 10)); // Max 10 results
        return userRepository.searchUsers(searchTerm.trim(), excludeUserId, pageable)
                .map(UserSearchResponse::fromEntity);
    }

    public UserResponse updateUser(UUID id, UserRequest request) {
        log.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        if (request.phoneNumber() != null && !request.phoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("User with phone number " + request.phoneNumber() + " already exists");
        }

        userMapper.updateEntityFromRequest(request, user);
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", updatedUser.getId());

        publishUserEvent(updatedUser, UserEvent.EventType.USER_UPDATED);

        return userMapper.toResponse(updatedUser);
    }

    public void deleteUser(UUID id) {
        log.info("Deleting user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        userRepository.delete(user);
        log.info("User deleted successfully with ID: {}", id);

        // Publish user deleted event
        publishUserEvent(user, UserEvent.EventType.USER_DELETED);
    }

    public UserResponse updateUserStatus(UUID id, User.UserStatus status) {
        log.info("Updating status for user ID: {} to {}", id, status);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        user.setStatus(status);
        User updatedUser = userRepository.save(user);

        publishUserEvent(updatedUser, UserEvent.EventType.USER_STATUS_CHANGED);

        return userMapper.toResponse(updatedUser);
    }

    public UserResponse verifyEmail(UUID id) {
        log.info("Verifying email for user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        user.setEmailVerified(true);
        if (user.getStatus() == User.UserStatus.PENDING_VERIFICATION) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        User updatedUser = userRepository.save(user);

        publishUserEvent(updatedUser, UserEvent.EventType.USER_EMAIL_VERIFIED);

        return userMapper.toResponse(updatedUser);
    }

    private void publishUserEvent(User user, UserEvent.EventType eventType) {
        UserEvent event = userMapper.toEvent(user, eventType);
        userEventProducer.sendUserEvent(event);
    }

    // ==================== Internal API Methods ====================
    // Used by auth-service for authentication operations

    /**
     * Get user entity by ID (includes password for auth verification).
     */
    @Transactional(readOnly = true)
    public User getUserEntityById(UUID id) {
        log.debug("Internal: Fetching user entity with ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    /**
     * Get user entity by email (includes password for auth verification).
     */
    @Transactional(readOnly = true)
    public Optional<User> findUserEntityByEmail(String email) {
        log.debug("Internal: Fetching user entity with email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Get user entity by email (throws if not found).
     */
    @Transactional(readOnly = true)
    public User getUserEntityByEmail(String email) {
        log.debug("Internal: Fetching user entity with email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Create user from auth-service (with pre-hashed password and auth provider info).
     */
    public User createUserFromAuth(CreateUserRequest request) {
        log.info("Internal: Creating user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        if (request.phoneNumber() != null && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("User with phone number " + request.phoneNumber() + " already exists");
        }

        User user = User.builder()
                .email(request.email())
                .password(request.password())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .status(User.UserStatus.valueOf(request.status()))
                .role(User.UserRole.valueOf(request.role()))
                .authProvider(request.authProvider() != null 
                        ? User.AuthProvider.valueOf(request.authProvider()) 
                        : User.AuthProvider.LOCAL)
                .providerId(request.providerId())
                .profileImageUrl(request.profileImageUrl())
                .emailVerified(request.emailVerified())
                .phoneVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Internal: User created with ID: {}", savedUser.getId());

        publishUserEvent(savedUser, UserEvent.EventType.USER_CREATED);

        return savedUser;
    }

    /**
     * Update last login timestamp.
     */
    public void updateLastLogin(UUID id) {
        log.debug("Internal: Updating last login for user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Check if email exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if phone number exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    /**
     * Link OAuth provider to existing user.
     */
    public User linkOAuthProvider(UUID id, String provider, String providerId, String profileImageUrl) {
        log.info("Internal: Linking OAuth provider {} for user: {}", provider, id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        user.setAuthProvider(User.AuthProvider.valueOf(provider));
        user.setProviderId(providerId);
        if (profileImageUrl != null && user.getProfileImageUrl() == null) {
            user.setProfileImageUrl(profileImageUrl);
        }

        return userRepository.save(user);
    }
}
