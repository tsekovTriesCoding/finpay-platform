package com.finpay.user.repository;

import com.finpay.user.entity.User;
import com.finpay.user.testconfig.TestMySQLContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMySQLContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("UserRepository Data JPA Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private User createUser(String email, String firstName, String lastName, String phone) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("encoded-password")
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phone)
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .plan(User.AccountPlan.STARTER)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
    }

    @Nested
    @DisplayName("Save and Find")
    class SaveAndFind {

        @Test
        @DisplayName("should save and find user by ID")
        void shouldSaveAndFindById() {
            User user = createUser("john@example.com", "John", "Doe", "+1234567890");
            User saved = userRepository.save(user);
            entityManager.flush();

            Optional<User> found = userRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("john@example.com");
            assertThat(found.get().getFirstName()).isEqualTo("John");
            assertThat(found.get().getCreatedAt()).isNotNull();
            assertThat(found.get().getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should auto-generate timestamps")
        void shouldAutoGenerateTimestamps() {
            User user = createUser("ts@example.com", "Time", "Stamp", "+9999999999");
            User saved = userRepository.save(user);
            entityManager.flush();

            User found = entityManager.find(User.class, saved.getId());
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Find By Email")
    class FindByEmail {

        @Test
        @DisplayName("should find user by email")
        void shouldFindByEmail() {
            User user = createUser("jane@example.com", "Jane", "Doe", "+111222333");
            userRepository.save(user);

            Optional<User> found = userRepository.findByEmail("jane@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("Jane");
        }

        @Test
        @DisplayName("should return empty for non-existent email")
        void shouldReturnEmptyForNonExistentEmail() {
            Optional<User> found = userRepository.findByEmail("nobody@example.com");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find By Phone Number")
    class FindByPhoneNumber {

        @Test
        @DisplayName("should find user by phone number")
        void shouldFindByPhoneNumber() {
            User user = createUser("phone@example.com", "Phone", "User", "+5551234567");
            userRepository.save(user);

            Optional<User> found = userRepository.findByPhoneNumber("+5551234567");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("phone@example.com");
        }
    }

    @Nested
    @DisplayName("Exists Checks")
    class ExistsChecks {

        @Test
        @DisplayName("should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            userRepository.save(createUser("exists@example.com", "Exists", "User", "+1111111111"));

            assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
        }

        @Test
        @DisplayName("should return false when email does not exist")
        void shouldReturnFalseWhenEmailNotExists() {
            assertThat(userRepository.existsByEmail("no@example.com")).isFalse();
        }

        @Test
        @DisplayName("should return true when phone exists")
        void shouldReturnTrueWhenPhoneExists() {
            userRepository.save(createUser("ph@example.com", "Ph", "User", "+2222222222"));

            assertThat(userRepository.existsByPhoneNumber("+2222222222")).isTrue();
        }

        @Test
        @DisplayName("should return false when phone does not exist")
        void shouldReturnFalseWhenPhoneNotExists() {
            assertThat(userRepository.existsByPhoneNumber("+0000000000")).isFalse();
        }
    }

    @Nested
    @DisplayName("Find By Status and Role")
    class FindByStatusAndRole {

        @Test
        @DisplayName("should find users by status")
        void shouldFindByStatus() {
            userRepository.save(createUser("a1@ex.com", "Active", "One", "+3333333331"));
            User suspended = createUser("s1@ex.com", "Suspended", "One", "+3333333332");
            suspended.setStatus(User.UserStatus.SUSPENDED);
            userRepository.save(suspended);

            List<User> activeUsers = userRepository.findByStatus(User.UserStatus.ACTIVE);
            List<User> suspendedUsers = userRepository.findByStatus(User.UserStatus.SUSPENDED);

            assertThat(activeUsers).hasSize(1);
            assertThat(suspendedUsers).hasSize(1);
            assertThat(suspendedUsers.get(0).getFirstName()).isEqualTo("Suspended");
        }

        @Test
        @DisplayName("should find users by role")
        void shouldFindByRole() {
            userRepository.save(createUser("u@ex.com", "Normal", "User", "+4444444441"));
            User admin = createUser("a@ex.com", "Admin", "User", "+4444444442");
            admin.setRole(User.UserRole.ADMIN);
            userRepository.save(admin);

            List<User> users = userRepository.findByRole(User.UserRole.USER);
            List<User> admins = userRepository.findByRole(User.UserRole.ADMIN);

            assertThat(users).hasSize(1);
            assertThat(admins).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Search Users")
    class SearchUsers {

        @Test
        @DisplayName("should search users by first name")
        void shouldSearchByFirstName() {
            UUID excludeId = UUID.randomUUID();
            userRepository.save(createUser("john@ex.com", "John", "Smith", "+5555555551"));
            userRepository.save(createUser("jane@ex.com", "Jane", "Doe", "+5555555552"));

            Page<User> results = userRepository.searchUsers("John", excludeId, PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("should search users by last name")
        void shouldSearchByLastName() {
            UUID excludeId = UUID.randomUUID();
            userRepository.save(createUser("a@ex.com", "Alice", "Johnson", "+6666666661"));
            userRepository.save(createUser("b@ex.com", "Bob", "Williams", "+6666666662"));

            Page<User> results = userRepository.searchUsers("Johnson", excludeId, PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).getEmail()).isEqualTo("a@ex.com");
        }

        @Test
        @DisplayName("should search users by email")
        void shouldSearchByEmail() {
            UUID excludeId = UUID.randomUUID();
            userRepository.save(createUser("special@company.com", "Special", "User", "+7777777771"));

            Page<User> results = userRepository.searchUsers("special@company", excludeId, PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should search by full name")
        void shouldSearchByFullName() {
            UUID excludeId = UUID.randomUUID();
            userRepository.save(createUser("jd@ex.com", "John", "Doe", "+8888888881"));

            Page<User> results = userRepository.searchUsers("John Doe", excludeId, PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should exclude current user from search results")
        void shouldExcludeCurrentUser() {
            User currentUser = createUser("me@ex.com", "John", "Current", "+8888888882");
            User savedCurrent = userRepository.save(currentUser);
            userRepository.save(createUser("other@ex.com", "John", "Other", "+8888888883"));

            Page<User> results = userRepository.searchUsers("John", savedCurrent.getId(), PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).getLastName()).isEqualTo("Other");
        }

        @Test
        @DisplayName("should only return active users in search")
        void shouldOnlyReturnActiveUsers() {
            UUID excludeId = UUID.randomUUID();
            userRepository.save(createUser("active@ex.com", "John", "Active", "+9999999991"));
            User suspended = createUser("susp@ex.com", "John", "Suspended", "+9999999992");
            suspended.setStatus(User.UserStatus.SUSPENDED);
            userRepository.save(suspended);

            Page<User> results = userRepository.searchUsers("John", excludeId, PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).getLastName()).isEqualTo("Active");
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            UUID excludeId = UUID.randomUUID();
            userRepository.save(createUser("cs@ex.com", "Alice", "Wonder", "+1010101010"));

            Page<User> results = userRepository.searchUsers("alice", excludeId, PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Account Plan Persistence")
    class AccountPlanTests {

        @Test
        @DisplayName("should persist and retrieve account plan")
        void shouldPersistAccountPlan() {
            User user = createUser("plan@ex.com", "Plan", "User", "+1212121212");
            user.setPlan(User.AccountPlan.PRO);
            userRepository.save(user);

            Optional<User> found = userRepository.findByEmail("plan@ex.com");

            assertThat(found).isPresent();
            assertThat(found.get().getPlan()).isEqualTo(User.AccountPlan.PRO);
        }

        @Test
        @DisplayName("should default to STARTER plan")
        void shouldDefaultToStarterPlan() {
            User user = createUser("starter@ex.com", "Starter", "User", "+1313131313");
            userRepository.save(user);

            Optional<User> found = userRepository.findByEmail("starter@ex.com");

            assertThat(found).isPresent();
            assertThat(found.get().getPlan()).isEqualTo(User.AccountPlan.STARTER);
        }
    }
}
