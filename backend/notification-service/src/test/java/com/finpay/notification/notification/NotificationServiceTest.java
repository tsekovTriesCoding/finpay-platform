package com.finpay.notification.notification;

import com.finpay.notification.notification.delivery.EmailService;
import com.finpay.notification.notification.delivery.WebSocketNotificationService;
import com.finpay.notification.notification.dto.NotificationRequest;
import com.finpay.notification.notification.dto.NotificationResponse;
import com.finpay.notification.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationMapper notificationMapper;
    @Mock private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks private NotificationService notificationService;

    private UUID userId;
    private UUID notificationId;
    private Notification testNotification;
    private NotificationResponse testResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        testNotification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(Notification.NotificationType.PAYMENT_COMPLETED)
                .channel(Notification.NotificationChannel.EMAIL)
                .subject("Payment Successful")
                .content("Your payment of $100 was completed.")
                .recipient("john@example.com")
                .status(Notification.NotificationStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testResponse = new NotificationResponse(
                notificationId, userId,
                Notification.NotificationType.PAYMENT_COMPLETED,
                Notification.NotificationChannel.EMAIL,
                "Payment Successful",
                "Your payment of $100 was completed.",
                "john@example.com",
                Notification.NotificationStatus.PENDING,
                null, null, null, LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("Create Notification")
    class CreateNotificationTests {

        @Test
        @DisplayName("should create notification from request")
        void shouldCreateNotification() {
            NotificationRequest request = new NotificationRequest(
                    userId, Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.EMAIL,
                    "Payment Successful", "Your payment was completed.",
                    "john@example.com", null
            );

            when(notificationMapper.toEntity(request)).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponse(testNotification)).thenReturn(testResponse);

            NotificationResponse response = notificationService.createNotification(request);

            assertThat(response).isNotNull();
            assertThat(response.type()).isEqualTo(Notification.NotificationType.PAYMENT_COMPLETED);
            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("Send Notification")
    class SendNotificationTests {

        @Test
        @DisplayName("should send email notification")
        void shouldSendEmailNotification() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            notificationService.sendNotification(testNotification);

            verify(emailService).sendEmail("john@example.com", "Payment Successful",
                    "Your payment of $100 was completed.");
            assertThat(testNotification.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
        }

        @Test
        @DisplayName("should send in-app notification via WebSocket")
        void shouldSendInAppNotification() {
            testNotification.setChannel(Notification.NotificationChannel.IN_APP);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponse(testNotification)).thenReturn(testResponse);

            notificationService.sendNotification(testNotification);

            verify(webSocketNotificationService).pushToUser(eq(userId), any(NotificationResponse.class));
        }

        @Test
        @DisplayName("should send push notification via WebSocket")
        void shouldSendPushNotification() {
            testNotification.setChannel(Notification.NotificationChannel.PUSH);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponse(testNotification)).thenReturn(testResponse);

            notificationService.sendNotification(testNotification);

            verify(webSocketNotificationService).pushToUser(eq(userId), any(NotificationResponse.class));
        }

        @Test
        @DisplayName("should handle send failure and increment retry count")
        void shouldHandleSendFailure() {
            doThrow(new RuntimeException("Mail server down"))
                    .when(emailService).sendEmail(anyString(), anyString(), anyString());
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            notificationService.sendNotification(testNotification);

            assertThat(testNotification.getStatus()).isEqualTo(Notification.NotificationStatus.FAILED);
            assertThat(testNotification.getErrorMessage()).contains("Mail server down");
            assertThat(testNotification.getRetryCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get Notifications")
    class GetNotificationTests {

        @Test
        @DisplayName("should get notification by ID")
        void shouldGetById() {
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));
            when(notificationMapper.toResponse(testNotification)).thenReturn(testResponse);

            NotificationResponse response = notificationService.getNotificationById(notificationId);

            assertThat(response.id()).isEqualTo(notificationId);
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.getNotificationById(notificationId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should get unread notifications")
        void shouldGetUnreadNotifications() {
            when(notificationRepository.findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                    userId, Notification.NotificationChannel.IN_APP))
                    .thenReturn(List.of(testNotification));
            when(notificationMapper.toResponse(testNotification)).thenReturn(testResponse);

            List<NotificationResponse> unread = notificationService.getUnreadNotifications(userId);

            assertThat(unread).hasSize(1);
        }

        @Test
        @DisplayName("should get unread count")
        void shouldGetUnreadCount() {
            when(notificationRepository.countByUserIdAndChannelAndReadAtIsNull(
                    userId, Notification.NotificationChannel.IN_APP)).thenReturn(5L);

            long count = notificationService.getUnreadCount(userId);

            assertThat(count).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Mark As Read")
    class MarkAsReadTests {

        @Test
        @DisplayName("should mark single notification as read")
        void shouldMarkAsRead() {
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponse(testNotification)).thenReturn(testResponse);
            when(notificationRepository.countByUserIdAndChannelAndReadAtIsNull(any(), any())).thenReturn(3L);

            notificationService.markAsRead(notificationId);

            assertThat(testNotification.getReadAt()).isNotNull();
            assertThat(testNotification.getStatus()).isEqualTo(Notification.NotificationStatus.READ);
            verify(webSocketNotificationService).pushUnreadCount(userId, 3L);
        }

        @Test
        @DisplayName("should mark all as read for user")
        void shouldMarkAllAsRead() {
            Notification n1 = Notification.builder()
                    .id(UUID.randomUUID()).userId(userId).channel(Notification.NotificationChannel.IN_APP)
                    .type(Notification.NotificationType.SYSTEM).subject("S").content("C")
                    .status(Notification.NotificationStatus.SENT).retryCount(0).build();
            Notification n2 = Notification.builder()
                    .id(UUID.randomUUID()).userId(userId).channel(Notification.NotificationChannel.IN_APP)
                    .type(Notification.NotificationType.SYSTEM).subject("S").content("C")
                    .status(Notification.NotificationStatus.SENT).retryCount(0).build();

            when(notificationRepository.findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                    userId, Notification.NotificationChannel.IN_APP))
                    .thenReturn(List.of(n1, n2));

            notificationService.markAllAsRead(userId);

            assertThat(n1.getReadAt()).isNotNull();
            assertThat(n2.getReadAt()).isNotNull();
            verify(notificationRepository).saveAll(anyList());
            verify(webSocketNotificationService).pushUnreadCount(userId, 0);
        }
    }

    @Nested
    @DisplayName("Delete Notification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("should delete notification")
        void shouldDeleteNotification() {
            when(notificationRepository.existsById(notificationId)).thenReturn(true);

            notificationService.deleteNotification(notificationId);

            verify(notificationRepository).deleteById(notificationId);
        }

        @Test
        @DisplayName("should throw when deleting non-existent notification")
        void shouldThrowWhenDeletingNonExistent() {
            when(notificationRepository.existsById(notificationId)).thenReturn(false);

            assertThatThrownBy(() -> notificationService.deleteNotification(notificationId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
