package com.finpay.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.notification.notification.Notification;
import com.finpay.notification.notification.NotificationRepository;
import com.finpay.notification.notification.delivery.WebSocketNotificationService;
import com.finpay.notification.notification.dto.NotificationRequest;
import com.finpay.notification.preference.NotificationPreferenceRepository;
import com.finpay.notification.preference.dto.NotificationPreferenceRequest;
import com.finpay.notification.testconfig.TestcontainersConfig;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@DisplayName("NotificationController Integration Tests")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @MockitoBean
    private WebSocketNotificationService webSocketNotificationService;

    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
    }

    private Notification createNotificationInDb(UUID userId, boolean read) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(Notification.NotificationType.PAYMENT_COMPLETED)
                .channel(Notification.NotificationChannel.IN_APP)
                .subject("Test Subject")
                .content("Test Content")
                .recipient("test@example.com")
                .status(Notification.NotificationStatus.SENT)
                .retryCount(0)
                .build();
        if (read) {
            notification.setReadAt(java.time.LocalDateTime.now());
        }
        return notificationRepository.save(notification);
    }

    @Nested
    @DisplayName("POST /api/v1/notifications")
    class CreateNotification {

        @Test
        @DisplayName("should create a notification")
        void shouldCreateNotification() throws Exception {
            NotificationRequest request = new NotificationRequest(
                    TEST_USER_ID,
                    Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.IN_APP,
                    "Payment Done",
                    "Your payment of $100 was completed",
                    "test@example.com",
                    null
            );

            MvcTestResult result = mvc.post().uri("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson()
                    .extractingPath("$.subject").isEqualTo("Payment Done");
            assertThat(result).bodyJson()
                    .extractingPath("$.userId").isEqualTo(TEST_USER_ID.toString());
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldRejectInvalidRequest() throws Exception {
            NotificationRequest invalid = new NotificationRequest(
                    null, null, null, "", "", null, null
            );

            assertThat(mvc.post().uri("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/{id}")
    class GetById {

        @Test
        @DisplayName("should return notification by ID")
        void shouldReturnNotificationById() {
            Notification notification = createNotificationInDb(TEST_USER_ID, false);

            assertThat(mvc.get().uri("/api/v1/notifications/{id}", notification.getId()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.subject").isEqualTo("Test Subject");
        }

        @Test
        @DisplayName("should return 404 for unknown ID")
        void shouldReturn404ForUnknownId() {
            assertThat(mvc.get().uri("/api/v1/notifications/{id}", UUID.randomUUID()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/user/{userId}")
    class GetByUser {

        @Test
        @DisplayName("should return notifications for user")
        void shouldReturnNotificationsForUser() {
            createNotificationInDb(TEST_USER_ID, false);
            createNotificationInDb(TEST_USER_ID, true);

            assertThat(mvc.get().uri("/api/v1/notifications/user/{userId}", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.length()").isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Unread Notifications")
    class UnreadNotifications {

        @Test
        @DisplayName("should return unread notifications")
        void shouldReturnUnreadNotifications() {
            createNotificationInDb(TEST_USER_ID, false);
            createNotificationInDb(TEST_USER_ID, false);
            createNotificationInDb(TEST_USER_ID, true);

            assertThat(mvc.get().uri("/api/v1/notifications/user/{userId}/unread", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.length()").isEqualTo(2);
        }

        @Test
        @DisplayName("should return unread count")
        void shouldReturnUnreadCount() {
            createNotificationInDb(TEST_USER_ID, false);
            createNotificationInDb(TEST_USER_ID, false);
            createNotificationInDb(TEST_USER_ID, true);

            assertThat(mvc.get().uri("/api/v1/notifications/user/{userId}/unread/count", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.count").isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Mark As Read")
    class MarkAsRead {

        @Test
        @DisplayName("should mark notification as read")
        void shouldMarkAsRead() {
            Notification notification = createNotificationInDb(TEST_USER_ID, false);

            assertThat(mvc.post().uri("/api/v1/notifications/{id}/read", notification.getId()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.readAt").isNotNull();
        }

        @Test
        @DisplayName("should mark all notifications as read")
        void shouldMarkAllAsRead() {
            createNotificationInDb(TEST_USER_ID, false);
            createNotificationInDb(TEST_USER_ID, false);

            assertThat(mvc.post().uri("/api/v1/notifications/user/{userId}/read-all", TEST_USER_ID))
                    .hasStatus(HttpStatus.NO_CONTENT);

            long unreadCount = notificationRepository.countByUserIdAndReadAtIsNull(TEST_USER_ID);
            assertThat(unreadCount).isZero();
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/notifications/{id}")
    class DeleteNotification {

        @Test
        @DisplayName("should delete notification")
        void shouldDeleteNotification() {
            Notification notification = createNotificationInDb(TEST_USER_ID, false);

            assertThat(mvc.delete().uri("/api/v1/notifications/{id}", notification.getId()))
                    .hasStatus(HttpStatus.NO_CONTENT);

            assertThat(notificationRepository.findById(notification.getId())).isEmpty();
        }

        @Test
        @DisplayName("should return 404 when deleting nonexistent notification")
        void shouldReturn404ForNonexistent() {
            assertThat(mvc.delete().uri("/api/v1/notifications/{id}", UUID.randomUUID()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/user/{userId}/paged")
    class PagedNotifications {

        @Test
        @DisplayName("should return paginated notifications")
        void shouldReturnPaginatedNotifications() {
            for (int i = 0; i < 5; i++) {
                createNotificationInDb(TEST_USER_ID, false);
            }

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/notifications/user/{userId}/paged?page=0&size=3", TEST_USER_ID)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(3);
            assertThat(result).bodyJson()
                    .extractingPath("$.totalElements").isEqualTo(5);
        }

        @Test
        @DisplayName("should return empty page for unknown user")
        void shouldReturnEmptyPageForUnknownUser() {
            assertThat(mvc.get().uri("/api/v1/notifications/user/{userId}/paged?page=0&size=10",
                    UUID.randomUUID()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content.length()").isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Notification Types")
    class NotificationTypes {

        @Test
        @DisplayName("should create notifications with different types")
        void shouldCreateDifferentTypes() throws Exception {
            NotificationRequest securityReq = new NotificationRequest(
                    TEST_USER_ID,
                    Notification.NotificationType.SECURITY_ALERT,
                    Notification.NotificationChannel.IN_APP,
                    "Security Alert",
                    "Unusual login detected",
                    null, null
            );

            MvcTestResult result = mvc.post().uri("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(securityReq))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson()
                    .extractingPath("$.type").isEqualTo("SECURITY_ALERT");
        }

        @Test
        @DisplayName("should filter unread to IN_APP channel only")
        void shouldFilterUnreadToInAppOnly() {
            // Create IN_APP notification (should appear in unread)
            createNotificationInDb(TEST_USER_ID, false);

            // Create EMAIL notification directly
            Notification emailNotification = Notification.builder()
                    .userId(TEST_USER_ID)
                    .type(Notification.NotificationType.PAYMENT_COMPLETED)
                    .channel(Notification.NotificationChannel.EMAIL)
                    .subject("Email Notification")
                    .content("Email content")
                    .recipient("user@example.com")
                    .status(Notification.NotificationStatus.SENT)
                    .retryCount(0)
                    .build();
            notificationRepository.save(emailNotification);

            // Only IN_APP should appear in unread
            assertThat(mvc.get().uri("/api/v1/notifications/user/{userId}/unread", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.length()").isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Mark As Read Edge Cases")
    class MarkAsReadEdgeCases {

        @Test
        @DisplayName("should return 404 when marking nonexistent notification as read")
        void shouldReturn404ForUnknown() {
            assertThat(mvc.post().uri("/api/v1/notifications/{id}/read", UUID.randomUUID()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should handle marking already-read notification as read")
        void shouldHandleAlreadyRead() {
            Notification notification = createNotificationInDb(TEST_USER_ID, true);

            // Should succeed (idempotent)
            assertThat(mvc.post().uri("/api/v1/notifications/{id}/read", notification.getId()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.readAt").isNotNull();
        }

        @Test
        @DisplayName("should update unread count after marking all as read")
        void shouldUpdateUnreadCountAfterMarkAll() {
            createNotificationInDb(TEST_USER_ID, false);
            createNotificationInDb(TEST_USER_ID, false);
            createNotificationInDb(TEST_USER_ID, false);

            // Initially 3 unread
            assertThat(mvc.get().uri("/api/v1/notifications/user/{userId}/unread/count", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.count").isEqualTo(3);

            // Mark all as read
            mvc.post().uri("/api/v1/notifications/user/{userId}/read-all", TEST_USER_ID).exchange();

            // Now 0 unread
            assertThat(mvc.get().uri("/api/v1/notifications/user/{userId}/unread/count", TEST_USER_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.count").isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Notification Preferences")
    class Preferences {

        @Test
        @DisplayName("should auto-create default preferences for new user")
        void shouldAutoCreateDefaultPreferences() {
            UUID newUserId = UUID.randomUUID();

            MvcTestResult result = mvc.get()
                    .uri("/api/v1/notifications/user/{userId}/preferences", newUserId)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.userId").isEqualTo(newUserId.toString());
            assertThat(result).bodyJson()
                    .extractingPath("$.emailEnabled").isEqualTo(true);
            assertThat(result).bodyJson()
                    .extractingPath("$.inAppEnabled").isEqualTo(true);
        }

        @Test
        @DisplayName("should update preferences")
        void shouldUpdatePreferences() throws Exception {
            UUID userId = UUID.randomUUID();
            // First auto-create
            mvc.get().uri("/api/v1/notifications/user/{userId}/preferences", userId).exchange();

            // Disable email and promotional
            NotificationPreferenceRequest request = new NotificationPreferenceRequest(
                    false, false, false, true,
                    true, true, false, true
            );

            MvcTestResult result = mvc.put()
                    .uri("/api/v1/notifications/user/{userId}/preferences", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.emailEnabled").isEqualTo(false);
            assertThat(result).bodyJson()
                    .extractingPath("$.promotionalNotifications").isEqualTo(false);
            assertThat(result).bodyJson()
                    .extractingPath("$.inAppEnabled").isEqualTo(true);
        }

        @Test
        @DisplayName("should persist updated preferences across requests")
        void shouldPersistUpdatedPreferences() throws Exception {
            UUID userId = UUID.randomUUID();
            // Auto-create
            mvc.get().uri("/api/v1/notifications/user/{userId}/preferences", userId).exchange();

            // Update
            NotificationPreferenceRequest request = new NotificationPreferenceRequest(
                    true, true, false, true,
                    true, true, true, false
            );
            mvc.put().uri("/api/v1/notifications/user/{userId}/preferences", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .exchange();

            // Retrieve and verify
            MvcTestResult result = mvc.get()
                    .uri("/api/v1/notifications/user/{userId}/preferences", userId)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.pushEnabled").isEqualTo(false);
            assertThat(result).bodyJson()
                    .extractingPath("$.systemNotifications").isEqualTo(false);
        }
    }
}
