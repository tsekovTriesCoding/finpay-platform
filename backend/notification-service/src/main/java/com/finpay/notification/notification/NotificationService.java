package com.finpay.notification.notification;

import com.finpay.notification.notification.delivery.EmailService;
import com.finpay.notification.notification.delivery.WebSocketNotificationService;
import com.finpay.notification.notification.dto.NotificationRequest;
import com.finpay.notification.notification.dto.NotificationResponse;
import com.finpay.notification.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final NotificationMapper notificationMapper;
    private final WebSocketNotificationService webSocketNotificationService;

    public NotificationResponse createNotification(NotificationRequest request) {
        log.info("Creating notification for user: {} type: {}", request.userId(), request.type());

        Notification notification = notificationMapper.toEntity(request);
        Notification saved = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", saved.getId());

        return notificationMapper.toResponse(saved);
    }

    public void createAndSendNotification(UUID userId, Notification.NotificationType type,
                                          Notification.NotificationChannel channel,
                                          String subject, String content, String recipient) {
        log.info("Creating and sending notification for user: {} type: {} channel: {}", userId, type, channel);

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .channel(channel)
                .subject(subject)
                .content(content)
                .recipient(recipient)
                .status(Notification.NotificationStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        sendNotification(saved);
    }

    public void sendNotification(Notification notification) {
        log.info("Sending notification: {} via {}", notification.getId(), notification.getChannel());

        try {
            notification.setStatus(Notification.NotificationStatus.SENDING);
            notificationRepository.save(notification);

            switch (notification.getChannel()) {
                case EMAIL -> sendEmailNotification(notification);
                case SMS -> sendSmsNotification(notification);
                case PUSH -> sendPushNotification(notification);
                case IN_APP -> markAsInApp(notification);
            }

            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Notification sent successfully: {}", notification.getId());

        } catch (Exception e) {
            log.error("Failed to send notification: {} - {}", notification.getId(), e.getMessage());
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepository.save(notification);
        }
    }

    private void sendEmailNotification(Notification notification) {
        if (notification.getRecipient() != null) {
            emailService.sendEmail(notification.getRecipient(), notification.getSubject(), notification.getContent());
        } else {
            log.warn("No email recipient for notification: {}", notification.getId());
        }
    }

    private void sendSmsNotification(Notification notification) {
        // TODO: SMS provider integration
        log.info("SMS notification would be sent to: {}", notification.getRecipient());
    }

    private void sendPushNotification(Notification notification) {
        log.info("Sending push notification to user: {} via WebSocket", notification.getUserId());
        NotificationResponse response = notificationMapper.toResponse(notification);
        webSocketNotificationService.pushToUser(notification.getUserId(), response);
    }

    private void markAsInApp(Notification notification) {
        log.info("In-app notification created for user: {}, pushing via WebSocket", notification.getUserId());
        NotificationResponse response = notificationMapper.toResponse(notification);
        webSocketNotificationService.pushToUser(notification.getUserId(), response);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));
        return notificationMapper.toResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(
                        userId, Notification.NotificationChannel.IN_APP).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByUserId(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndChannel(
                        userId, Notification.NotificationChannel.IN_APP, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                        userId, Notification.NotificationChannel.IN_APP).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndChannelAndReadAtIsNull(
                userId, Notification.NotificationChannel.IN_APP);
    }

    public NotificationResponse markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        notification.setReadAt(LocalDateTime.now());
        notification.setStatus(Notification.NotificationStatus.READ);
        Notification updated = notificationRepository.save(notification);

        long unreadCount = notificationRepository.countByUserIdAndChannelAndReadAtIsNull(
                notification.getUserId(), Notification.NotificationChannel.IN_APP);
        webSocketNotificationService.pushUnreadCount(notification.getUserId(), unreadCount);

        return notificationMapper.toResponse(updated);
    }

    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                userId, Notification.NotificationChannel.IN_APP);
        LocalDateTime now = LocalDateTime.now();

        unread.forEach(notification -> {
            notification.setReadAt(now);
            notification.setStatus(Notification.NotificationStatus.READ);
        });

        notificationRepository.saveAll(unread);
        log.info("Marked {} notifications as read for user: {}", unread.size(), userId);

        webSocketNotificationService.pushUnreadCount(userId, 0);
    }

    public void deleteNotification(UUID notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification not found with ID: " + notificationId);
        }
        notificationRepository.deleteById(notificationId);
    }
}
