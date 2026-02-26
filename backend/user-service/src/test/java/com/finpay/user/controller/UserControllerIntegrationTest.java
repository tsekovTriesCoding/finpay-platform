package com.finpay.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.user.dto.CreateUserRequest;
import com.finpay.user.dto.UserRequest;
import com.finpay.user.entity.User;
import com.finpay.user.repository.UserRepository;
import com.finpay.user.service.CloudinaryService;
import com.finpay.user.testconfig.TestcontainersConfig;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private String uniquePhone() {
        return "+1" + String.format("%09d", PHONE_SEQ.incrementAndGet());
    }

    private User createUserInDb(String email) {
        User user = User.builder()
                .email(email)
                .password("hashed")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber(uniquePhone())
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .emailVerified(false)
                .phoneVerified(false)
                .plan(User.AccountPlan.STARTER)
                .build();
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("POST /api/v1/internal/users")
    class CreateUser {

        @Test
        @DisplayName("should create a new user via internal endpoint")
        void shouldCreateUser() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "create@test.com", "hashedPw123", "John", "Doe",
                    "+10000000001", "ACTIVE", "USER", "LOCAL", null, null, false);

            MvcTestResult result = mvc.post().uri("/api/v1/internal/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson()
                    .extractingPath("$.email").isEqualTo("create@test.com");
            assertThat(result).bodyJson()
                    .extractingPath("$.firstName").isEqualTo("John");

            assertThat(userRepository.findByEmail("create@test.com")).isPresent();
        }

        @Test
        @DisplayName("should return 409 for duplicate email")
        void shouldRejectDuplicateEmail() throws Exception {
            createUserInDb("dup@test.com");

            CreateUserRequest request = new CreateUserRequest(
                    "dup@test.com", "hashedPw123", "Jane", "Doe",
                    null, "ACTIVE", "USER", "LOCAL", null, null, false);

            assertThat(mvc.post().uri("/api/v1/internal/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserById {

        @Test
        @DisplayName("should return user by ID")
        void shouldReturnUserById() {
            User user = createUserInDb("getid@test.com");

            assertThat(mvc.get().uri("/api/v1/users/{id}", user.getId()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.email").isEqualTo("getid@test.com");
        }

        @Test
        @DisplayName("should return 404 for unknown ID")
        void shouldReturn404ForUnknownId() {
            assertThat(mvc.get().uri("/api/v1/users/{id}", UUID.randomUUID()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/email/{email}")
    class GetUserByEmail {

        @Test
        @DisplayName("should return user by email")
        void shouldReturnUserByEmail() {
            createUserInDb("getemail@test.com");

            assertThat(mvc.get().uri("/api/v1/users/email/{email}", "getemail@test.com"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.email").isEqualTo("getemail@test.com");
        }

        @Test
        @DisplayName("should return 404 for unknown email")
        void shouldReturn404ForUnknownEmail() {
            assertThat(mvc.get().uri("/api/v1/users/email/{email}", "nope@test.com"))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAllUsers {

        @Test
        @DisplayName("should return all users")
        void shouldReturnAllUsers() {
            createUserInDb("first@test.com");
            createUserInDb("second@test.com");

            assertThat(mvc.get().uri("/api/v1/users"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.length()").isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("should update user details")
        void shouldUpdateUser() throws Exception {
            User user = createUserInDb("update@test.com");

            UserRequest updateReq = new UserRequest("update@test.com", "Jane", "Smith",
                    "+9876543210", null, "456 Oak Ave", "Portland", "US", "97201");

            MvcTestResult result = mvc.put().uri("/api/v1/users/{id}", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateReq))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.firstName").isEqualTo("Jane");
            assertThat(result).bodyJson()
                    .extractingPath("$.lastName").isEqualTo("Smith");
            assertThat(result).bodyJson()
                    .extractingPath("$.city").isEqualTo("Portland");
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("should delete user")
        void shouldDeleteUser() {
            User user = createUserInDb("delete@test.com");

            assertThat(mvc.delete().uri("/api/v1/users/{id}", user.getId()))
                    .hasStatus(HttpStatus.NO_CONTENT);

            assertThat(userRepository.findById(user.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("should update user status")
        void shouldUpdateStatus() {
            User user = createUserInDb("status@test.com");

            assertThat(mvc.patch().uri("/api/v1/users/{id}/status?status=SUSPENDED", user.getId()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.status").isEqualTo("SUSPENDED");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/{id}/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("should verify user email")
        void shouldVerifyEmail() {
            User user = createUserInDb("verify@test.com");

            assertThat(mvc.post().uri("/api/v1/users/{id}/verify-email", user.getId()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.emailVerified").isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Internal User API")
    class InternalUserApi {

        @Test
        @DisplayName("should get internal user by email")
        void shouldGetInternalUserByEmail() {
            createUserInDb("internalemail@test.com");

            assertThat(mvc.get().uri("/api/v1/internal/users/email/{email}", "internalemail@test.com"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.email").isEqualTo("internalemail@test.com");
        }

        @Test
        @DisplayName("should check email exists")
        void shouldCheckEmailExists() {
            createUserInDb("exists@test.com");

            assertThat(mvc.get().uri("/api/v1/internal/users/exists/email/{email}", "exists@test.com"))
                    .hasStatusOk()
                    .hasBodyTextEqualTo("true");
        }

        @Test
        @DisplayName("should return false for nonexistent email")
        void shouldReturnFalseForNonexistentEmail() {
            assertThat(mvc.get().uri("/api/v1/internal/users/exists/email/{email}", "nope@test.com"))
                    .hasStatusOk()
                    .hasBodyTextEqualTo("false");
        }

        @Test
        @DisplayName("should update last login")
        void shouldUpdateLastLogin() {
            User user = createUserInDb("lastlogin@test.com");

            assertThat(mvc.patch().uri("/api/v1/internal/users/{id}/last-login", user.getId()))
                    .hasStatusOk();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/health")
    class Health {

        @Test
        @DisplayName("should return health status")
        void shouldReturnHealth() {
            assertThat(mvc.get().uri("/api/v1/users/health"))
                    .hasStatusOk();
        }
    }
}
