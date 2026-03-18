package com.finpay.user.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.user.audit.AuditLog;
import com.finpay.user.audit.AuditLogRepository;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@DisplayName("AdminUserController Integration Tests")
class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(900_000);
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final String ADMIN_EMAIL = "admin@finpay.com";

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String uniquePhone() {
        return "+1" + String.format("%09d", PHONE_SEQ.incrementAndGet());
    }

    private User createUserInDb(String email, User.UserStatus status, User.UserRole role) {
        User user = User.builder()
                .email(email)
                .password("hashed")
                .firstName("Test")
                .lastName("User")
                .phoneNumber(uniquePhone())
                .status(status)
                .role(role)
                .emailVerified(true)
                .phoneVerified(false)
                .plan(User.AccountPlan.STARTER)
                .build();
        return userRepository.save(user);
    }

    private User createActiveUser(String email) {
        return createUserInDb(email, User.UserStatus.ACTIVE, User.UserRole.USER);
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class ListUsers {

        @Test
        @DisplayName("should return paginated list of all users")
        void shouldReturnAllUsers() {
            createActiveUser("alice@test.com");
            createActiveUser("bob@test.com");
            createActiveUser("charlie@test.com");

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users?page=0&size=10")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(3);
            assertThat(result).bodyJson()
                    .extractingPath("$.page.totalElements").isEqualTo(3);
        }

        @Test
        @DisplayName("should filter users by status")
        void shouldFilterByStatus() {
            createActiveUser("active@test.com");
            createUserInDb("suspended@test.com", User.UserStatus.SUSPENDED, User.UserRole.USER);

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users?status=SUSPENDED")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].email").isEqualTo("suspended@test.com");
        }

        @Test
        @DisplayName("should filter users by role")
        void shouldFilterByRole() {
            createActiveUser("user@test.com");
            createUserInDb("merchant@test.com", User.UserStatus.ACTIVE, User.UserRole.MERCHANT);

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users?role=MERCHANT")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].email").isEqualTo("merchant@test.com");
        }

        @Test
        @DisplayName("should filter by both role and status")
        void shouldFilterByRoleAndStatus() {
            createUserInDb("active-user@test.com", User.UserStatus.ACTIVE, User.UserRole.USER);
            createUserInDb("suspended-user@test.com", User.UserStatus.SUSPENDED, User.UserRole.USER);
            createUserInDb("active-merchant@test.com", User.UserStatus.ACTIVE, User.UserRole.MERCHANT);

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users?role=USER&status=SUSPENDED")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].email").isEqualTo("suspended-user@test.com");
        }

        @Test
        @DisplayName("should search users by name or email")
        void shouldSearchUsers() {
            createActiveUser("alice@test.com");
            createActiveUser("bob@test.com");

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users?search=alice")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].email").isEqualTo("alice@test.com");
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void shouldRespectPagination() {
            for (int i = 1; i <= 5; i++) {
                createActiveUser("user" + i + "@test.com");
            }

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users?page=0&size=2")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(2);
            assertThat(result).bodyJson()
                    .extractingPath("$.page.totalElements").isEqualTo(5);
            assertThat(result).bodyJson()
                    .extractingPath("$.page.totalPages").isEqualTo(3);
        }

        @Test
        @DisplayName("should return empty page when no users match")
        void shouldReturnEmptyPage() {
            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users?status=SUSPENDED")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(0);
            assertThat(result).bodyJson()
                    .extractingPath("$.page.totalElements").isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/users/{userId}/role")
    class ChangeRole {

        @Test
        @DisplayName("should change user role from USER to ADMIN")
        void shouldChangeRole() throws Exception {
            User user = createActiveUser("promote@test.com");

            MvcTestResult result = mvc.patch()
                    .uri("/api/v1/admin/users/{userId}/role", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN")))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.role").isEqualTo("ADMIN");

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getRole()).isEqualTo(User.UserRole.ADMIN);
        }

        @Test
        @DisplayName("should create audit log when role changes")
        void shouldCreateAuditLog() throws Exception {
            User user = createActiveUser("audit-role@test.com");

            mvc.patch()
                    .uri("/api/v1/admin/users/{userId}/role", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("role", "MERCHANT")))
                    .exchange();

            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.getFirst().getAction()).isEqualTo(AuditLog.AuditAction.USER_ROLE_CHANGED);
            assertThat(logs.getFirst().getActorEmail()).isEqualTo(ADMIN_EMAIL);
            assertThat(logs.getFirst().getPreviousState()).isEqualTo("USER");
            assertThat(logs.getFirst().getNewState()).isEqualTo("MERCHANT");
        }

        @Test
        @DisplayName("should return 404 for non-existent user")
        void shouldReturn404ForNonExistentUser() throws Exception {
            MvcTestResult result = mvc.patch()
                    .uri("/api/v1/admin/users/{userId}/role", UUID.randomUUID())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN")))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users/{userId}/suspend")
    class SuspendUser {

        @Test
        @DisplayName("should suspend an active user")
        void shouldSuspendUser() throws Exception {
            User user = createActiveUser("suspend-me@test.com");

            MvcTestResult result = mvc.post()
                    .uri("/api/v1/admin/users/{userId}/suspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("reason", "Violation of TOS")))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("SUSPENDED");

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(User.UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("should create audit log for suspension")
        void shouldCreateSuspensionAuditLog() throws Exception {
            User user = createActiveUser("audit-suspend@test.com");

            mvc.post()
                    .uri("/api/v1/admin/users/{userId}/suspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("reason", "Fraud detected")))
                    .exchange();

            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.getFirst().getAction()).isEqualTo(AuditLog.AuditAction.USER_SUSPENDED);
            assertThat(logs.getFirst().getDescription()).contains("Fraud detected");
            assertThat(logs.getFirst().getNewState()).isEqualTo("SUSPENDED");
        }

        @Test
        @DisplayName("should suspend without reason body")
        void shouldSuspendWithoutReason() {
            User user = createActiveUser("no-reason@test.com");

            MvcTestResult result = mvc.post()
                    .uri("/api/v1/admin/users/{userId}/suspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("SUSPENDED");
        }

        @Test
        @DisplayName("should return 404 for non-existent user")
        void shouldReturn404ForNonExistentUser() throws Exception {
            MvcTestResult result = mvc.post()
                    .uri("/api/v1/admin/users/{userId}/suspend", UUID.randomUUID())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("reason", "Test")))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users/{userId}/unsuspend")
    class UnsuspendUser {

        @Test
        @DisplayName("should unsuspend a suspended user")
        void shouldUnsuspendUser() {
            User user = createUserInDb("unsuspend-me@test.com", User.UserStatus.SUSPENDED, User.UserRole.USER);

            MvcTestResult result = mvc.post()
                    .uri("/api/v1/admin/users/{userId}/unsuspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("ACTIVE");

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("should create audit log for unsuspension")
        void shouldCreateUnsuspendAuditLog() {
            User user = createUserInDb("audit-unsuspend@test.com", User.UserStatus.SUSPENDED, User.UserRole.USER);

            mvc.post()
                    .uri("/api/v1/admin/users/{userId}/unsuspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .exchange();

            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.getFirst().getAction()).isEqualTo(AuditLog.AuditAction.USER_UNSUSPENDED);
            assertThat(logs.getFirst().getPreviousState()).isEqualTo("SUSPENDED");
            assertThat(logs.getFirst().getNewState()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users/{userId}/force-password-reset")
    class ForcePasswordReset {

        @Test
        @DisplayName("should force password reset and set status to PENDING_VERIFICATION")
        void shouldForcePasswordReset() {
            User user = createActiveUser("reset-me@test.com");

            MvcTestResult result = mvc.post()
                    .uri("/api/v1/admin/users/{userId}/force-password-reset", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.status").isEqualTo("PENDING_VERIFICATION");

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(User.UserStatus.PENDING_VERIFICATION);
        }

        @Test
        @DisplayName("should create audit log for force password reset")
        void shouldCreateForceResetAuditLog() {
            User user = createActiveUser("audit-reset@test.com");

            mvc.post()
                    .uri("/api/v1/admin/users/{userId}/force-password-reset", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .exchange();

            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.getFirst().getAction()).isEqualTo(AuditLog.AuditAction.USER_FORCE_PASSWORD_RESET);
            assertThat(logs.getFirst().getTargetId()).isEqualTo(user.getId().toString());
        }

        @Test
        @DisplayName("should return 404 for non-existent user")
        void shouldReturn404ForNonExistentUser() {
            assertThat(mvc.post()
                    .uri("/api/v1/admin/users/{userId}/force-password-reset", UUID.randomUUID())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/by-role/{role}")
    class UsersByRole {

        @Test
        @DisplayName("should return users matching the requested role")
        void shouldReturnUsersByRole() {
            createUserInDb("admin1@test.com", User.UserStatus.ACTIVE, User.UserRole.ADMIN);
            createUserInDb("admin2@test.com", User.UserStatus.ACTIVE, User.UserRole.ADMIN);
            createActiveUser("regular@test.com");

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users/by-role/ADMIN")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.length()").isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty list when no users have the role")
        void shouldReturnEmptyForNoMatches() {
            createActiveUser("only-user@test.com");

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users/by-role/MERCHANT")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.length()").isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/dashboard/metrics")
    class DashboardMetrics {

        @Test
        @DisplayName("should return correct KPI metrics")
        void shouldReturnMetrics() {
            createUserInDb("active1@test.com", User.UserStatus.ACTIVE, User.UserRole.USER);
            createUserInDb("active2@test.com", User.UserStatus.ACTIVE, User.UserRole.USER);
            createUserInDb("suspended1@test.com", User.UserStatus.SUSPENDED, User.UserRole.USER);
            createUserInDb("admin1@test.com", User.UserStatus.ACTIVE, User.UserRole.ADMIN);
            createUserInDb("merchant1@test.com", User.UserStatus.ACTIVE, User.UserRole.MERCHANT);
            createUserInDb("pending1@test.com", User.UserStatus.PENDING_VERIFICATION, User.UserRole.USER);

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users/dashboard/metrics")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalUsers").isEqualTo(6);
            assertThat(result).bodyJson()
                    .extractingPath("$.activeUsers").isEqualTo(4);
            assertThat(result).bodyJson()
                    .extractingPath("$.suspendedUsers").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.pendingVerification").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.adminCount").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.merchantCount").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.regularUserCount").isEqualTo(4);
        }

        @Test
        @DisplayName("should return zero metrics when no users exist")
        void shouldReturnZeroMetrics() {
            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users/dashboard/metrics")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalUsers").isEqualTo(0);
            assertThat(result).bodyJson()
                    .extractingPath("$.activeUsers").isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Audit Log Endpoints")
    class AuditLogs {

        @Test
        @DisplayName("should return paginated audit logs")
        void shouldReturnAuditLogs() throws Exception {
            User user = createActiveUser("audit-target@test.com");

            mvc.post()
                    .uri("/api/v1/admin/users/{userId}/suspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("reason", "Test")))
                    .exchange();

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users/audit-logs?page=0&size=10")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].action").isEqualTo("USER_SUSPENDED");
        }

        @Test
        @DisplayName("should search audit logs by action filter")
        void shouldSearchAuditLogsByAction() throws Exception {
            User user1 = createActiveUser("action-filter1@test.com");
            User user2 = createActiveUser("action-filter2@test.com");

            mvc.post()
                    .uri("/api/v1/admin/users/{userId}/suspend", user1.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("reason", "Test")))
                    .exchange();

            mvc.patch()
                    .uri("/api/v1/admin/users/{userId}/role", user2.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("role", "MERCHANT")))
                    .exchange();

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users/audit-logs/search?action=USER_SUSPENDED")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$.content[0].action").isEqualTo("USER_SUSPENDED");
        }

        @Test
        @DisplayName("should return audit logs by target type and ID")
        void shouldReturnAuditLogsByTarget() throws Exception {
            User user = createActiveUser("target-logs@test.com");

            mvc.post()
                    .uri("/api/v1/admin/users/{userId}/suspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("reason", "Test")))
                    .exchange();

            mvc.post()
                    .uri("/api/v1/admin/users/{userId}/unsuspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .exchange();

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users/audit-logs/target/USER/{targetId}",
                            user.getId().toString())
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty audit logs when none exist")
        void shouldReturnEmptyAuditLogs() {
            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users/audit-logs?page=0&size=10")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("End-to-End Admin Workflows")
    class EndToEndWorkflows {

        @Test
        @DisplayName("should suspend and then unsuspend a user with full audit trail")
        void shouldSuspendAndUnsuspendWithAuditTrail() {
            User user = createActiveUser("lifecycle@test.com");

            mvc.post()
                    .uri("/api/v1/admin/users/{userId}/suspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .exchange();

            User afterSuspend = userRepository.findById(user.getId()).orElseThrow();
            assertThat(afterSuspend.getStatus()).isEqualTo(User.UserStatus.SUSPENDED);

            mvc.post()
                    .uri("/api/v1/admin/users/{userId}/unsuspend", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .exchange();

            User afterUnsuspend = userRepository.findById(user.getId()).orElseThrow();
            assertThat(afterUnsuspend.getStatus()).isEqualTo(User.UserStatus.ACTIVE);

            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(2);
            assertThat(logs).extracting(AuditLog::getAction)
                    .containsExactlyInAnyOrder(
                            AuditLog.AuditAction.USER_SUSPENDED,
                            AuditLog.AuditAction.USER_UNSUSPENDED
                    );
        }

        @Test
        @DisplayName("should change role and verify it appears in by-role listing")
        void shouldChangeRoleAndVerifyListing() throws Exception {
            User user = createActiveUser("role-upgrade@test.com");

            mvc.patch()
                    .uri("/api/v1/admin/users/{userId}/role", user.getId())
                    .header("X-User-Id", ADMIN_ID.toString())
                    .header("X-User-Email", ADMIN_EMAIL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("role", "MERCHANT")))
                    .exchange();

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/admin/users/by-role/MERCHANT")
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.length()").isEqualTo(1);
            assertThat(result).bodyJson()
                    .extractingPath("$[0].email").isEqualTo("role-upgrade@test.com");
        }
    }
}
