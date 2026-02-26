package com.finpay.notification.notification;

import com.finpay.notification.testconfig.TestMySQLContainerConfig;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMySQLContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("NotificationRepository Data JPA Tests")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    private Notification createNotification(UUID userId, Notification.NotificationType type,
                                             Notification.NotificationChannel channel,
                                             Notification.NotificationStatus status) {
        return Notification.builder()
                .userId(userId)
                .type(type)
                .channel(channel)
                .subject("Test Subject")
                .content("Test Content")
                .recipient("test@example.com")
                .status(status)
                .retryCount(0)
                .build();
    }

    @Nested
    @DisplayName("Save and Find")
    class SaveAndFind {

        @Test
        @DisplayName("should save and find notification")
        void shouldSaveAndFind() {
            Notification notification = createNotification(
                    UUID.randomUUID(), Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.EMAIL, Notification.NotificationStatus.PENDING
            );

            Notification saved = notificationRepository.save(notification);
            entityManager.flush();

            assertThat(saved.getId()).isNotNull();

            Notification found = entityManager.find(Notification.class, saved.getId());
            assertThat(found.getCreatedAt()).isNotNull();

            Optional<Notification> foundOpt = notificationRepository.findById(saved.getId());
            assertThat(foundOpt).isPresent();
            assertThat(foundOpt.get().getSubject()).isEqualTo("Test Subject");
        }
    }

    @Nested
    @DisplayName("Find By UserId")
    class FindByUserId {

        @Test
        @DisplayName("should find notifications by user ID")
        void shouldFindByUserId() {
            UUID userId = UUID.randomUUID();
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.EMAIL,
                    Notification.NotificationStatus.SENT));
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.TRANSFER_SENT,
                    Notification.NotificationChannel.IN_APP,
                    Notification.NotificationStatus.SENT));
            notificationRepository.save(createNotification(UUID.randomUUID(),
                    Notification.NotificationType.SYSTEM,
                    Notification.NotificationChannel.EMAIL,
                    Notification.NotificationStatus.SENT));

            List<Notification> results = notificationRepository.findByUserId(userId);

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Unread Notifications")
    class UnreadNotifications {

        @Test
        @DisplayName("should find unread in-app notifications")
        void shouldFindUnreadInApp() {
            UUID userId = UUID.randomUUID();

            // Unread in-app
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.IN_APP,
                    Notification.NotificationStatus.SENT));

            // Read in-app
            Notification read = createNotification(userId,
                    Notification.NotificationType.TRANSFER_SENT,
                    Notification.NotificationChannel.IN_APP,
                    Notification.NotificationStatus.READ);
            read.setReadAt(LocalDateTime.now());
            notificationRepository.save(read);

            // Unread email (different channel)
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.SYSTEM,
                    Notification.NotificationChannel.EMAIL,
                    Notification.NotificationStatus.SENT));

            List<Notification> unread = notificationRepository
                    .findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                            userId, Notification.NotificationChannel.IN_APP);

            assertThat(unread).hasSize(1);
            assertThat(unread.get(0).getType()).isEqualTo(Notification.NotificationType.PAYMENT_COMPLETED);
        }

        @Test
        @DisplayName("should count unread in-app notifications")
        void shouldCountUnreadInApp() {
            UUID userId = UUID.randomUUID();

            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.IN_APP,
                    Notification.NotificationStatus.SENT));
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.TRANSFER_SENT,
                    Notification.NotificationChannel.IN_APP,
                    Notification.NotificationStatus.SENT));

            long count = notificationRepository.countByUserIdAndChannelAndReadAtIsNull(
                    userId, Notification.NotificationChannel.IN_APP);

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Find By Status")
    class FindByStatus {

        @Test
        @DisplayName("should find notifications by status")
        void shouldFindByStatus() {
            UUID userId = UUID.randomUUID();
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.EMAIL,
                    Notification.NotificationStatus.FAILED));
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.TRANSFER_SENT,
                    Notification.NotificationChannel.EMAIL,
                    Notification.NotificationStatus.SENT));

            List<Notification> failed = notificationRepository.findByStatus(
                    Notification.NotificationStatus.FAILED);

            assertThat(failed).hasSize(1);
        }

        @Test
        @DisplayName("should find failed notifications for retry")
        void shouldFindFailedForRetry() {
            UUID userId = UUID.randomUUID();

            Notification retryable = createNotification(userId,
                    Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.EMAIL,
                    Notification.NotificationStatus.FAILED);
            retryable.setRetryCount(1);
            notificationRepository.save(retryable);

            Notification maxRetries = createNotification(userId,
                    Notification.NotificationType.TRANSFER_SENT,
                    Notification.NotificationChannel.EMAIL,
                    Notification.NotificationStatus.FAILED);
            maxRetries.setRetryCount(5);
            notificationRepository.save(maxRetries);

            List<Notification> retryable_list = notificationRepository.findByStatusAndRetryCountLessThan(
                    Notification.NotificationStatus.FAILED, 3);

            assertThat(retryable_list).hasSize(1);
            assertThat(retryable_list.get(0).getRetryCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Find By Channel")
    class FindByChannel {

        @Test
        @DisplayName("should find by user and channel")
        void shouldFindByUserAndChannel() {
            UUID userId = UUID.randomUUID();
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.EMAIL,
                    Notification.NotificationStatus.SENT));
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.TRANSFER_SENT,
                    Notification.NotificationChannel.IN_APP,
                    Notification.NotificationStatus.SENT));
            notificationRepository.save(createNotification(userId,
                    Notification.NotificationType.SYSTEM,
                    Notification.NotificationChannel.IN_APP,
                    Notification.NotificationStatus.SENT));

            List<Notification> inApp = notificationRepository
                    .findByUserIdAndChannelOrderByCreatedAtDesc(
                            userId, Notification.NotificationChannel.IN_APP);

            assertThat(inApp).hasSize(2);
        }
    }
}
