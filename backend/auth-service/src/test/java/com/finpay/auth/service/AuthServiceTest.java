package com.finpay.auth.service;

import com.finpay.auth.dto.*;
import com.finpay.auth.entity.AccountPlan;
import com.finpay.auth.entity.RefreshToken;
import com.finpay.auth.entity.UserCredential;
import com.finpay.auth.exception.InvalidPlanUpgradeException;
import com.finpay.auth.exception.InvalidTokenException;
import com.finpay.auth.exception.PlanAlreadyActiveException;
import com.finpay.auth.exception.UserAlreadyExistsException;
import com.finpay.auth.kafka.AuthEventProducer;
import com.finpay.auth.repository.RefreshTokenRepository;
import com.finpay.auth.repository.UserCredentialRepository;
import com.finpay.auth.security.JwtService;
import com.finpay.auth.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserCredentialRepository credentialRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthEventProducer authEventProducer;
    @Mock private UserServiceClient userServiceClient;
    @Mock private TokenBlocklistService tokenBlocklistService;
    @Mock private UserSessionCacheService sessionCacheService;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserCredential savedCredential;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "john@example.com", "password123", "John", "Doe",
                "+1234567890", AccountPlan.STARTER
        );

        loginRequest = new LoginRequest("john@example.com", "password123");

        savedCredential = UserCredential.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .plan(AccountPlan.STARTER)
                .enabled(true)
                .accountLocked(false)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterUserSuccessfully() {
            when(credentialRepository.existsByEmail(anyString())).thenReturn(false);
            when(credentialRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(credentialRepository.save(any(UserCredential.class))).thenReturn(savedCredential);
            when(jwtService.generateAccessToken(any(UserDto.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(UserDto.class))).thenReturn("refresh-token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(RefreshToken.builder().build());

            AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.user()).isNotNull();
            assertThat(response.user().email()).isEqualTo("john@example.com");
            assertThat(response.user().firstName()).isEqualTo("John");
            assertThat(response.user().lastName()).isEqualTo("Doe");

            verify(credentialRepository).save(any(UserCredential.class));
            verify(authEventProducer).publishUserRegistered(any());
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowWhenEmailExists() {
            when(credentialRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("john@example.com");

            verify(credentialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when phone number already exists")
        void shouldThrowWhenPhoneNumberExists() {
            when(credentialRepository.existsByEmail(anyString())).thenReturn(false);
            when(credentialRepository.existsByPhoneNumber("+1234567890")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("phone number");

            verify(credentialRepository, never()).save(any());
        }

        @Test
        @DisplayName("should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            when(credentialRepository.existsByEmail(anyString())).thenReturn(false);
            when(credentialRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
            when(credentialRepository.save(any(UserCredential.class))).thenReturn(savedCredential);
            when(jwtService.generateAccessToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(refreshTokenRepository.save(any())).thenReturn(RefreshToken.builder().build());

            authService.register(registerRequest);

            ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
            verify(credentialRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$encoded");
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully with correct credentials")
        void shouldLoginSuccessfully() {
            when(credentialRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedCredential));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(credentialRepository.save(any(UserCredential.class))).thenReturn(savedCredential);
            when(jwtService.generateAccessToken(any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(refreshTokenRepository.save(any())).thenReturn(RefreshToken.builder().build());

            AuthResponse response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.user().email()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should throw exception when email not found")
        void shouldThrowWhenEmailNotFound() {
            when(credentialRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("should throw exception when password is incorrect")
        void shouldThrowWhenPasswordIncorrect() {
            when(credentialRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedCredential));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("should throw exception when account is suspended")
        void shouldThrowWhenAccountSuspended() {
            savedCredential.setEnabled(false);
            when(credentialRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedCredential));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Account is suspended");
        }

        @Test
        @DisplayName("should throw exception when account is locked")
        void shouldThrowWhenAccountLocked() {
            savedCredential.setAccountLocked(true);
            when(credentialRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedCredential));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Account is locked");
        }

        @Test
        @DisplayName("should update last login time on successful login")
        void shouldUpdateLastLoginTime() {
            when(credentialRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedCredential));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(credentialRepository.save(any(UserCredential.class))).thenReturn(savedCredential);
            when(jwtService.generateAccessToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(refreshTokenRepository.save(any())).thenReturn(RefreshToken.builder().build());

            authService.login(loginRequest);

            assertThat(savedCredential.getLastLoginAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("should reject refresh when account is suspended")
        void shouldRejectRefreshWhenSuspended() {
            savedCredential.setEnabled(false);

            RefreshToken validToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash(TokenHashUtil.sha256("suspended-user-token"))
                    .userId(savedCredential.getId())
                    .userEmail("john@example.com")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256("suspended-user-token"))).thenReturn(Optional.of(validToken));
            when(credentialRepository.findById(savedCredential.getId())).thenReturn(Optional.of(savedCredential));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);

            RefreshTokenRequest request = new RefreshTokenRequest("suspended-user-token");
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Account is suspended");

            // Should revoke the token used in the attempt
            assertThat(validToken.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(validToken);
        }

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            RefreshToken validToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash(TokenHashUtil.sha256("valid-refresh-token"))
                    .userId(savedCredential.getId())
                    .userEmail("john@example.com")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256("valid-refresh-token"))).thenReturn(Optional.of(validToken));
            when(credentialRepository.findById(savedCredential.getId())).thenReturn(Optional.of(savedCredential));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);
            when(jwtService.generateAccessToken(any())).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh-token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);

            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            AuthResponse response = authService.refreshToken(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(validToken.isRevoked()).isTrue(); // Old token should be revoked
        }

        @Test
        @DisplayName("should throw exception for invalid refresh token")
        void shouldThrowForInvalidRefreshToken() {
            when(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256("invalid-token"))).thenReturn(Optional.empty());

            RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("should throw exception for expired refresh token")
        void shouldThrowForExpiredRefreshToken() {
            RefreshToken expiredToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash(TokenHashUtil.sha256("expired-token"))
                    .userId(savedCredential.getId())
                    .userEmail("john@example.com")
                    .expiryDate(LocalDateTime.now().minusDays(1))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256("expired-token"))).thenReturn(Optional.of(expiredToken));

            RefreshTokenRequest request = new RefreshTokenRequest("expired-token");
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Refresh token is expired or revoked");
        }
    }

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("should logout successfully by revoking refresh token")
        void shouldLogoutSuccessfully() {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash(TokenHashUtil.sha256("refresh-token"))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256("refresh-token"))).thenReturn(Optional.of(token));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);

            authService.logout("refresh-token", null);

            assertThat(token.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("should logout all sessions for a user")
        void shouldLogoutAllSessions() {
            UUID userId = UUID.randomUUID();

            authService.logoutAll(userId);

            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Upgrade Plan")
    class UpgradePlanTests {

        @Test
        @DisplayName("should upgrade plan from STARTER to PRO")
        void shouldUpgradeFromStarterToPro() {
            savedCredential.setPlan(AccountPlan.STARTER);
            when(credentialRepository.findById(savedCredential.getId())).thenReturn(Optional.of(savedCredential));
            when(credentialRepository.save(any(UserCredential.class))).thenReturn(savedCredential);

            UpgradePlanRequest request = new UpgradePlanRequest(AccountPlan.PRO);
            UpgradePlanResponse response = authService.upgradePlan(savedCredential.getId(), request);

            assertThat(response).isNotNull();
            assertThat(response.previousPlan()).isEqualTo("STARTER");
            assertThat(response.newPlan()).isEqualTo("PRO");
            assertThat(savedCredential.getPlan()).isEqualTo(AccountPlan.PRO);
            verify(authEventProducer).publishPlanUpgraded(any());
        }

        @Test
        @DisplayName("should upgrade plan from PRO to ENTERPRISE")
        void shouldUpgradeFromProToEnterprise() {
            savedCredential.setPlan(AccountPlan.PRO);
            when(credentialRepository.findById(savedCredential.getId())).thenReturn(Optional.of(savedCredential));
            when(credentialRepository.save(any(UserCredential.class))).thenReturn(savedCredential);

            UpgradePlanRequest request = new UpgradePlanRequest(AccountPlan.ENTERPRISE);
            UpgradePlanResponse response = authService.upgradePlan(savedCredential.getId(), request);

            assertThat(response.previousPlan()).isEqualTo("PRO");
            assertThat(response.newPlan()).isEqualTo("ENTERPRISE");
        }

        @Test
        @DisplayName("should throw exception when trying to downgrade plan")
        void shouldThrowWhenDowngradingPlan() {
            savedCredential.setPlan(AccountPlan.PRO);
            when(credentialRepository.findById(savedCredential.getId())).thenReturn(Optional.of(savedCredential));

            UpgradePlanRequest request = new UpgradePlanRequest(AccountPlan.STARTER);
            assertThatThrownBy(() -> authService.upgradePlan(savedCredential.getId(), request))
                    .isInstanceOf(InvalidPlanUpgradeException.class)
                    .hasMessageContaining("Cannot downgrade");
        }

        @Test
        @DisplayName("should throw exception when plan is already active")
        void shouldThrowWhenPlanAlreadyActive() {
            savedCredential.setPlan(AccountPlan.STARTER);
            when(credentialRepository.findById(savedCredential.getId())).thenReturn(Optional.of(savedCredential));

            UpgradePlanRequest request = new UpgradePlanRequest(AccountPlan.STARTER);
            assertThatThrownBy(() -> authService.upgradePlan(savedCredential.getId(), request))
                    .isInstanceOf(PlanAlreadyActiveException.class)
                    .hasMessageContaining("already on the STARTER plan");
        }
    }

    @Nested
    @DisplayName("Get Current User")
    class GetCurrentUserTests {

        @Test
        @DisplayName("should reject suspended user")
        void shouldRejectSuspendedUser() {
            savedCredential.setEnabled(false);

            when(jwtService.isTokenValid("valid-token")).thenReturn(true);
            when(jwtService.extractUserId("valid-token")).thenReturn(savedCredential.getId().toString());
            when(jwtService.extractExpiration("valid-token")).thenReturn(new java.util.Date(System.currentTimeMillis() + 60000));
            when(tokenBlocklistService.isBlocked(anyString())).thenReturn(false);
            when(jwtService.extractUserIdAsUUID("valid-token")).thenReturn(savedCredential.getId());
            when(sessionCacheService.getCachedSession(savedCredential.getId())).thenReturn(null);
            when(credentialRepository.findById(savedCredential.getId())).thenReturn(Optional.of(savedCredential));

            assertThatThrownBy(() -> authService.getCurrentUser("valid-token"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Account is suspended");

            verify(userServiceClient, never()).getUserProfile(any());
        }

        @Test
        @DisplayName("should return user profile for active user")
        void shouldReturnProfileForActiveUser() {
            when(jwtService.isTokenValid("valid-token")).thenReturn(true);
            when(jwtService.extractUserId("valid-token")).thenReturn(savedCredential.getId().toString());
            when(jwtService.extractExpiration("valid-token")).thenReturn(new java.util.Date(System.currentTimeMillis() + 60000));
            when(tokenBlocklistService.isBlocked(anyString())).thenReturn(false);
            when(jwtService.extractUserIdAsUUID("valid-token")).thenReturn(savedCredential.getId());
            when(sessionCacheService.getCachedSession(savedCredential.getId())).thenReturn(null);
            when(credentialRepository.findById(savedCredential.getId())).thenReturn(Optional.of(savedCredential));

            UserDto fullProfile = new UserDto(
                    savedCredential.getId(), "john@example.com", null,
                    "John", "Doe", "+1234567890", "ACTIVE", "USER",
                    null, null, null, null, null, null, null,
                    true, false, "STARTER", null, null, null
            );
            when(userServiceClient.getUserProfile(savedCredential.getId())).thenReturn(fullProfile);

            UserDto result = authService.getCurrentUser("valid-token");

            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should fall back to local credential when user-service is unavailable")
        void shouldFallBackToLocalCredential() {
            when(jwtService.isTokenValid("valid-token")).thenReturn(true);
            when(jwtService.extractUserId("valid-token")).thenReturn(savedCredential.getId().toString());
            when(jwtService.extractExpiration("valid-token")).thenReturn(new java.util.Date(System.currentTimeMillis() + 60000));
            when(tokenBlocklistService.isBlocked(anyString())).thenReturn(false);
            when(jwtService.extractUserIdAsUUID("valid-token")).thenReturn(savedCredential.getId());
            when(sessionCacheService.getCachedSession(savedCredential.getId())).thenReturn(null);
            when(credentialRepository.findById(savedCredential.getId())).thenReturn(Optional.of(savedCredential));
            when(userServiceClient.getUserProfile(savedCredential.getId())).thenReturn(null);

            UserDto result = authService.getCurrentUser("valid-token");

            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo("john@example.com");
            assertThat(result.role()).isEqualTo("USER");
        }

        @Test
        @DisplayName("should throw for invalid token")
        void shouldThrowForInvalidToken() {
            when(jwtService.isTokenValid("bad-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.getCurrentUser("bad-token"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid or expired token");
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            UUID missingId = UUID.randomUUID();
            when(jwtService.isTokenValid("valid-token")).thenReturn(true);
            when(jwtService.extractUserId("valid-token")).thenReturn(missingId.toString());
            when(jwtService.extractExpiration("valid-token")).thenReturn(new java.util.Date(System.currentTimeMillis() + 60000));
            when(tokenBlocklistService.isBlocked(anyString())).thenReturn(false);
            when(jwtService.extractUserIdAsUUID("valid-token")).thenReturn(missingId);
            when(sessionCacheService.getCachedSession(missingId)).thenReturn(null);
            when(credentialRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser("valid-token"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("User not found");
        }
    }
}
