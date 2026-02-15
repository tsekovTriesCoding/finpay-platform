package com.finpay.notification.notification.delivery;

import com.finpay.notification.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Pushes notifications to connected WebSocket clients in real-time.
 *
 * Uses Spring's user-destination system:
 *   - Server sends to: /user/{userId}/queue/notifications
 *   - Client subscribes to: /user/queue/notifications (resolved automatically)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Push a notification to a specific user via WebSocket.
     * If the user is not connected, the message is silently dropped.
     */
    public void pushToUser(UUID userId, NotificationResponse notification) {
        log.debug("Pushing notification {} to user {} via WebSocket", notification.id(), userId);

        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
            log.info("WebSocket push sent to user: {} notification: {}", userId, notification.id());
        } catch (Exception e) {
            // WebSocket push is best-effort — don't fail the main notification flow
            log.warn("Failed to push notification via WebSocket to user {}: {}", userId, e.getMessage());
        }
    }

    public void pushUnreadCount(UUID userId, long count) {
        log.debug("Pushing unread count {} to user {} via WebSocket", count, userId);

        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/unread-count",
                    new UnreadCountPayload(count)
            );
        } catch (Exception e) {
            log.warn("Failed to push unread count via WebSocket to user {}: {}", userId, e.getMessage());
        }
    }

    public record UnreadCountPayload(long count) {}
}
