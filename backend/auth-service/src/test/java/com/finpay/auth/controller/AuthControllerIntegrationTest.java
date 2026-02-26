package com.finpay.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.auth.dto.LoginRequest;
import com.finpay.auth.dto.RegisterRequest;
import com.finpay.auth.dto.UpgradePlanRequest;
import com.finpay.auth.entity.AccountPlan;
import com.finpay.auth.entity.RefreshToken;
import com.finpay.auth.entity.UserCredential;
import com.finpay.auth.repository.RefreshTokenRepository;
import com.finpay.auth.repository.UserCredentialRepository;
import com.finpay.auth.service.UserServiceClient;
import com.finpay.auth.testconfig.TestcontainersConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        credentialRepository.deleteAll();
        when(userServiceClient.getUserProfile(any())).thenReturn(null);
    }

    private RegisterRequest validRegisterRequest(String email) {
        return new RegisterRequest(email, "Password123!", "John", "Doe", null, AccountPlan.STARTER);
    }

    /**
     * Insert a test user credential directly into the DB,
     * bypassing the register API (avoids creating a refresh token
     * that can collide with a subsequent login/refresh JWT within
     * the same second).
     */
    private UserCredential insertTestUser(String email) {
        UserCredential credential = UserCredential.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .emailVerified(false)
                .accountLocked(false)
                .plan(AccountPlan.STARTER)
                .build();
        return credentialRepository.save(credential);
    }

    private String extractCookie(MvcTestResult result, String cookieName) {
        List<String> cookies = result.getResponse().getHeaders("Set-Cookie");
        for (String header : cookies) {
            if (header.startsWith(cookieName + "=")) {
                return header.split(";")[0].substring(cookieName.length() + 1);
            }
        }
        return null;
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("should register a new user and return 201 with cookies")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = validRegisterRequest("register@test.com");

            MvcTestResult result = mvc.post().uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CREATED);

            List<String> cookieHeaders = result.getResponse().getHeaders("Set-Cookie");
            assertThat(cookieHeaders).anyMatch(c -> c.startsWith("access_token="));
            assertThat(cookieHeaders).anyMatch(c -> c.startsWith("refresh_token="));

            assertThat(result).bodyJson()
                    .extractingPath("$.user.email").isEqualTo("register@test.com");
            assertThat(result).bodyJson()
                    .extractingPath("$.user.firstName").isEqualTo("John");

            assertThat(credentialRepository.existsByEmail("register@test.com")).isTrue();
        }

        @Test
        @DisplayName("should return 409 for duplicate email")
        void shouldRejectDuplicateEmail() throws Exception {
            RegisterRequest request = validRegisterRequest("duplicate@test.com");

            mvc.post().uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(mvc.post().uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("should return 400 for invalid request body")
        void shouldRejectInvalidRequest() throws Exception {
            RegisterRequest invalid = new RegisterRequest("not-an-email", "short", "", "", null, null);

            assertThat(mvc.post().uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("should login with valid credentials and return cookies")
        void shouldLoginWithValidCredentials() throws Exception {
            // Insert user directly (no refresh token created yet)
            insertTestUser("login@test.com");

            LoginRequest loginReq = new LoginRequest("login@test.com", "Password123!");

            MvcTestResult result = mvc.post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginReq))
                    .exchange();

            assertThat(result).hasStatusOk();
            List<String> cookieHeaders = result.getResponse().getHeaders("Set-Cookie");
            assertThat(cookieHeaders).anyMatch(c -> c.startsWith("access_token="));
            assertThat(cookieHeaders).anyMatch(c -> c.startsWith("refresh_token="));
            assertThat(result).bodyJson()
                    .extractingPath("$.user.email").isEqualTo("login@test.com");
        }

        @Test
        @DisplayName("should return 401 for wrong password")
        void shouldRejectWrongPassword() throws Exception {
            insertTestUser("wrongpw@test.com");

            LoginRequest loginReq = new LoginRequest("wrongpw@test.com", "WrongPassword!");

            assertThat(mvc.post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginReq)))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should return 401 for nonexistent user")
        void shouldRejectNonexistentUser() throws Exception {
            LoginRequest loginReq = new LoginRequest("noone@test.com", "Password123!");

            assertThat(mvc.post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginReq)))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenEndpoint {

        @Test
        @DisplayName("should refresh token with valid cookie")
        void shouldRefreshWithValidCookie() throws Exception {
            // Insert user and manually create a refresh token in DB
            UserCredential user = insertTestUser("refresh@test.com");
            String tokenValue = "test-refresh-token-" + UUID.randomUUID();
            RefreshToken token = RefreshToken.builder()
                    .token(tokenValue)
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(token);

            MvcTestResult result = mvc.post().uri("/api/v1/auth/refresh")
                    .cookie(new Cookie("refresh_token", tokenValue))
                    .exchange();

            assertThat(result).hasStatusOk();
            List<String> cookieHeaders = result.getResponse().getHeaders("Set-Cookie");
            assertThat(cookieHeaders).anyMatch(c -> c.startsWith("access_token="));
        }

        @Test
        @DisplayName("should return 401 when no refresh token cookie")
        void shouldRejectWithoutCookie() {
            assertThat(mvc.post().uri("/api/v1/auth/refresh"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("should logout and clear cookies")
        void shouldLogout() throws Exception {
            UserCredential user = insertTestUser("logout@test.com");
            String tokenValue = "test-refresh-token-" + UUID.randomUUID();
            RefreshToken token = RefreshToken.builder()
                    .token(tokenValue)
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(token);

            MvcTestResult result = mvc.post().uri("/api/v1/auth/logout")
                    .cookie(new Cookie("refresh_token", tokenValue))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.message").isEqualTo("Successfully logged out");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/validate")
    class ValidateToken {

        @Test
        @DisplayName("should validate a valid token from cookie")
        void shouldValidateValidToken() throws Exception {
            // Login to get a real access token
            insertTestUser("validate@test.com");
            MvcTestResult loginResult = mvc.post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new LoginRequest("validate@test.com", "Password123!")))
                    .exchange();

            String accessToken = extractCookie(loginResult, "access_token");
            assertThat(accessToken).isNotNull();

            MvcTestResult result = mvc.get().uri("/api/v1/auth/validate")
                    .cookie(new Cookie("access_token", accessToken))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.valid").isEqualTo(true);
            assertThat(result).bodyJson()
                    .extractingPath("$.email").isEqualTo("validate@test.com");
        }

        @Test
        @DisplayName("should return valid=false without token")
        void shouldReturnInvalidWithoutToken() {
            assertThat(mvc.get().uri("/api/v1/auth/validate"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.valid").isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class GetCurrentUser {

        @Test
        @DisplayName("should return current user profile")
        void shouldReturnCurrentUser() throws Exception {
            insertTestUser("me@test.com");
            MvcTestResult loginResult = mvc.post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new LoginRequest("me@test.com", "Password123!")))
                    .exchange();

            String accessToken = extractCookie(loginResult, "access_token");
            assertThat(accessToken).isNotNull();

            MvcTestResult result = mvc.get().uri("/api/v1/auth/me")
                    .cookie(new Cookie("access_token", accessToken))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.email").isEqualTo("me@test.com");
            assertThat(result).bodyJson()
                    .extractingPath("$.firstName").isEqualTo("John");
        }

        @Test
        @DisplayName("should return 401 without token")
        void shouldReturn401WithoutToken() {
            assertThat(mvc.get().uri("/api/v1/auth/me"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/auth/plan")
    class UpgradePlan {

        @Test
        @DisplayName("should upgrade plan from STARTER to PRO")
        void shouldUpgradePlan() throws Exception {
            insertTestUser("plan@test.com");
            MvcTestResult loginResult = mvc.post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new LoginRequest("plan@test.com", "Password123!")))
                    .exchange();

            String accessToken = extractCookie(loginResult, "access_token");
            assertThat(accessToken).isNotNull();

            UpgradePlanRequest upgradeRequest = new UpgradePlanRequest(AccountPlan.PRO);

            MvcTestResult result = mvc.put().uri("/api/v1/auth/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(upgradeRequest))
                    .cookie(new Cookie("access_token", accessToken))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.previousPlan").isEqualTo("STARTER");
            assertThat(result).bodyJson()
                    .extractingPath("$.newPlan").isEqualTo("PRO");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/health")
    class Health {

        @Test
        @DisplayName("should return health status")
        void shouldReturnHealth() {
            assertThat(mvc.get().uri("/api/v1/auth/health"))
                    .hasStatusOk()
                    .hasBodyTextEqualTo("Auth Service is running");
        }
    }
}
