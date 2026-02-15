package com.finpay.notification.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Intercepts STOMP CONNECT frames to extract the userId header
 * and set it as the authenticated Principal for user-specific messaging.
 *
 * The frontend sends: CONNECT with header "userId: <uuid>"
 * This enables SimpMessagingTemplate.convertAndSendToUser(userId, ...)
 */
@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = accessor.getFirstNativeHeader("userId");

            if (userId != null && !userId.isBlank()) {
                log.info("WebSocket CONNECT from user: {}", userId);
                accessor.setUser(new StompPrincipal(userId));
            } else {
                log.warn("WebSocket CONNECT without userId header");
            }
        }

        return message;
    }

    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
