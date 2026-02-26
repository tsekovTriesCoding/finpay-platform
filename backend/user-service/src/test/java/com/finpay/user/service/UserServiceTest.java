package com.finpay.user.service;

import com.finpay.user.dto.UserRequest;
import com.finpay.user.dto.UserResponse;
import com.finpay.user.dto.UserSearchResponse;
import com.finpay.user.entity.User;
import com.finpay.user.event.UserEvent;
import com.finpay.user.exception.ResourceNotFoundException;
import com.finpay.user.mapper.UserMapper;
import com.finpay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserEventProducer userEventProducer;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserService userService;

    private UUID userId;
    private User testUser;
    private UserRequest userRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("john@example.com")
                .password("encoded")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .plan(User.AccountPlan.STARTER)
                .emailVerified(false)
                .phoneVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRequest = new UserRequest(
                "john@example.com", "John", "Doe",
                "+1234567890", null, null, null, null, null
        );

        userResponse = new UserResponse(
                userId, "john@example.com", "John", "Doe", "+1234567890",
                User.UserStatus.ACTIVE, User.UserRole.USER, null,
                null, null, null, null, false, false,
                User.AccountPlan.STARTER, LocalDateTime.now(), LocalDateTime.now(), null
        );
    }

    @Nested
    @DisplayName("Get User")
    class GetUserTests {

        @Test
        @DisplayName("should get user by ID")
        void shouldGetUserById() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            UserResponse response = userService.getUserById(userId);

            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should throw when user not found by ID")
        void shouldThrowWhenNotFoundById() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(userId.toString());
        }

        @Test
        @DisplayName("should get user by email")
        void shouldGetUserByEmail() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            UserResponse response = userService.getUserByEmail("john@example.com");

            assertThat(response.email()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should get all users")
        void shouldGetAllUsers() {
            when(userRepository.findAll()).thenReturn(List.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            List<UserResponse> responses = userService.getAllUsers();

            assertThat(responses).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Search Users")
    class SearchUserTests {

        @Test
        @DisplayName("should search users by term")
        void shouldSearchUsers() {
            UUID excludeId = UUID.randomUUID();
            Page<User> userPage = new PageImpl<>(List.of(testUser));
            when(userRepository.searchUsers(eq("john"), eq(excludeId), any(Pageable.class)))
                    .thenReturn(userPage);

            Page<UserSearchResponse> results = userService.searchUsers("john", excludeId, 0, 5);

            assertThat(results).isNotEmpty();
        }

        @Test
        @DisplayName("should return empty page for null search term")
        void shouldReturnEmptyForNullSearch() {
            Page<UserSearchResponse> results = userService.searchUsers(null, UUID.randomUUID(), 0, 5);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty page for blank search term")
        void shouldReturnEmptyForBlankSearch() {
            Page<UserSearchResponse> results = userService.searchUsers("  ", UUID.randomUUID(), 0, 5);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update User")
    class UpdateUserTests {

        @Test
        @DisplayName("should update user successfully")
        void shouldUpdateUserSuccessfully() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);
            when(userMapper.toEvent(any(User.class), any(UserEvent.EventType.class)))
                    .thenReturn(new UserEvent(userId, "john@example.com", "John", "Doe", "STARTER",
                            UserEvent.EventType.USER_UPDATED, LocalDateTime.now()));

            UserResponse response = userService.updateUser(userId, userRequest);

            assertThat(response).isNotNull();
            verify(userMapper).updateEntityFromRequest(eq(userRequest), eq(testUser));
        }

        @Test
        @DisplayName("should throw when updating non-existent user")
        void shouldThrowWhenUpdatingNonExistentUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(userId, userRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete User")
    class DeleteUserTests {

        @Test
        @DisplayName("should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userMapper.toEvent(any(User.class), any(UserEvent.EventType.class)))
                    .thenReturn(new UserEvent(userId, "john@example.com", "John", "Doe", "STARTER",
                            UserEvent.EventType.USER_DELETED, LocalDateTime.now()));

            userService.deleteUser(userId);

            verify(userRepository).delete(testUser);
            verify(userEventProducer).sendUserEvent(any(UserEvent.class));
        }

        @Test
        @DisplayName("should throw when deleting non-existent user")
        void shouldThrowWhenDeletingNonExistentUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Status")
    class UpdateStatusTests {

        @Test
        @DisplayName("should update user status")
        void shouldUpdateStatus() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);
            when(userMapper.toEvent(any(User.class), any(UserEvent.EventType.class)))
                    .thenReturn(new UserEvent(userId, "john@example.com", "John", "Doe", "STARTER",
                            UserEvent.EventType.USER_STATUS_CHANGED, LocalDateTime.now()));

            UserResponse response = userService.updateUserStatus(userId, User.UserStatus.SUSPENDED);

            assertThat(testUser.getStatus()).isEqualTo(User.UserStatus.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("Email Verification")
    class EmailVerificationTests {

        @Test
        @DisplayName("should verify email and activate user")
        void shouldVerifyEmailAndActivateUser() {
            testUser.setStatus(User.UserStatus.PENDING_VERIFICATION);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);
            when(userMapper.toEvent(any(User.class), any(UserEvent.EventType.class)))
                    .thenReturn(new UserEvent(userId, "john@example.com", "John", "Doe", "STARTER",
                            UserEvent.EventType.USER_EMAIL_VERIFIED, LocalDateTime.now()));

            userService.verifyEmail(userId);

            assertThat(testUser.isEmailVerified()).isTrue();
            assertThat(testUser.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Exists Checks")
    class ExistsChecksTests {

        @Test
        @DisplayName("should check email exists")
        void shouldCheckEmailExists() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThat(userService.existsByEmail("john@example.com")).isTrue();
        }

        @Test
        @DisplayName("should check phone number exists")
        void shouldCheckPhoneExists() {
            when(userRepository.existsByPhoneNumber("+1234567890")).thenReturn(true);

            assertThat(userService.existsByPhoneNumber("+1234567890")).isTrue();
        }
    }
}
