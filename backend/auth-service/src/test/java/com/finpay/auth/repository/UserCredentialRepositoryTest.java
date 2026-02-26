package com.finpay.auth.repository;

import com.finpay.auth.entity.AccountPlan;
import com.finpay.auth.entity.UserCredential;
import com.finpay.auth.testconfig.TestMySQLContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMySQLContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("UserCredentialRepository Data JPA Tests")
class UserCredentialRepositoryTest {

    @Autowired
    private UserCredentialRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private UserCredential testCredential;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        testCredential = UserCredential.builder()
                .email("john@example.com")
                .passwordHash("$2a$10$encoded")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .plan(AccountPlan.STARTER)
                .enabled(true)
                .accountLocked(false)
                .emailVerified(false)
                .build();
    }

    @Nested
    @DisplayName("Save and Find")
    class SaveAndFindTests {

        @Test
        @DisplayName("should save and find user credential by ID")
        void shouldSaveAndFindById() {
            UserCredential saved = repository.save(testCredential);

            Optional<UserCredential> found = repository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("john@example.com");
            assertThat(found.get().getFirstName()).isEqualTo("John");
            assertThat(found.get().getLastName()).isEqualTo("Doe");
            assertThat(found.get().getPlan()).isEqualTo(AccountPlan.STARTER);
        }

        @Test
        @DisplayName("should auto-generate UUID on save")
        void shouldAutoGenerateUUID() {
            UserCredential saved = repository.save(testCredential);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("should set timestamps on save")
        void shouldSetTimestamps() {
            UserCredential saved = repository.save(testCredential);
            entityManager.flush();

            UserCredential found = entityManager.find(UserCredential.class, saved.getId());
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Find by Email")
    class FindByEmailTests {

        @Test
        @DisplayName("should find user credential by email")
        void shouldFindByEmail() {
            repository.save(testCredential);

            Optional<UserCredential> found = repository.findByEmail("john@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should return empty for non-existent email")
        void shouldReturnEmptyForNonExistentEmail() {
            Optional<UserCredential> found = repository.findByEmail("nonexistent@example.com");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exists checks")
    class ExistsTests {

        @Test
        @DisplayName("should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            repository.save(testCredential);

            assertThat(repository.existsByEmail("john@example.com")).isTrue();
        }

        @Test
        @DisplayName("should return false when email does not exist")
        void shouldReturnFalseWhenEmailNotExists() {
            assertThat(repository.existsByEmail("nonexistent@example.com")).isFalse();
        }

        @Test
        @DisplayName("should return true when phone number exists")
        void shouldReturnTrueWhenPhoneExists() {
            repository.save(testCredential);

            assertThat(repository.existsByPhoneNumber("+1234567890")).isTrue();
        }

        @Test
        @DisplayName("should return false when phone number does not exist")
        void shouldReturnFalseWhenPhoneNotExists() {
            assertThat(repository.existsByPhoneNumber("+9999999999")).isFalse();
        }
    }

    @Nested
    @DisplayName("OAuth lookup")
    class OAuthLookupTests {

        @Test
        @DisplayName("should find by OAuth provider and provider ID")
        void shouldFindByOAuthProviderAndProviderId() {
            testCredential.setOauthProvider("GOOGLE");
            testCredential.setOauthProviderId("google-123");
            repository.save(testCredential);

            Optional<UserCredential> found = repository.findByOauthProviderAndOauthProviderId("GOOGLE", "google-123");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should return empty for non-matching OAuth provider")
        void shouldReturnEmptyForNonMatchingOAuth() {
            testCredential.setOauthProvider("GOOGLE");
            testCredential.setOauthProviderId("google-123");
            repository.save(testCredential);

            Optional<UserCredential> found = repository.findByOauthProviderAndOauthProviderId("GITHUB", "google-123");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Account Plan")
    class AccountPlanTests {

        @Test
        @DisplayName("should default to STARTER plan")
        void shouldDefaultToStarterPlan() {
            UserCredential credential = UserCredential.builder()
                    .email("default@example.com")
                    .passwordHash("hash")
                    .enabled(true)
                    .accountLocked(false)
                    .emailVerified(false)
                    .build();

            UserCredential saved = repository.save(credential);

            assertThat(saved.getPlan()).isEqualTo(AccountPlan.STARTER);
        }

        @Test
        @DisplayName("should persist PRO plan")
        void shouldPersistProPlan() {
            testCredential.setPlan(AccountPlan.PRO);
            UserCredential saved = repository.save(testCredential);

            Optional<UserCredential> found = repository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getPlan()).isEqualTo(AccountPlan.PRO);
        }

        @Test
        @DisplayName("should persist ENTERPRISE plan")
        void shouldPersistEnterprisePlan() {
            testCredential.setPlan(AccountPlan.ENTERPRISE);
            UserCredential saved = repository.save(testCredential);

            Optional<UserCredential> found = repository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getPlan()).isEqualTo(AccountPlan.ENTERPRISE);
        }
    }
}
