import { useEffect, useRef, useCallback, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';

import { notificationKeys } from './useNotifications';
import type { Notification } from '../api/notificationApi';

const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8083/ws';

interface UseWebSocketOptions {
  userId: string;
  onNotification?: (notification: Notification) => void;
}

interface UseWebSocketReturn {
  isConnected: boolean;
}

/**
 * Manages a STOMP-over-WebSocket connection for real-time notifications.
 *
 * - Connects on mount, disconnects on unmount
 * - Auto-reconnects with exponential backoff (built into @stomp/stompjs)
 * - Invalidates TanStack Query caches when notifications arrive
 * - Calls onNotification callback for toast display
 */
export function useWebSocket({ userId, onNotification }: UseWebSocketOptions): UseWebSocketReturn {
  const [isConnected, setIsConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const queryClient = useQueryClient();

  const onNotificationRef = useRef(onNotification);
  onNotificationRef.current = onNotification;

  const handleIncomingNotification = useCallback(
    (notification: Notification) => {
      queryClient.setQueryData<Notification[]>(
        notificationKeys.byUser(userId),
        (old) => (old ? [notification, ...old] : [notification]),
      );

      queryClient.setQueryData<number>(
        notificationKeys.unreadCount(userId),
        (old) => (old ?? 0) + 1,
      );

      queryClient.invalidateQueries({ queryKey: notificationKeys.unread(userId) });

      onNotificationRef.current?.(notification);
    },
    [userId, queryClient],
  );

  const handleUnreadCountUpdate = useCallback(
    (payload: { count: number }) => {
      queryClient.setQueryData(notificationKeys.unreadCount(userId), payload.count);
    },
    [userId, queryClient],
  );

  useEffect(() => {
    if (!userId) return;

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: {
        userId,
      },
      reconnectDelay: 1000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: (msg) => {
        if (import.meta.env.DEV) {
          if (!msg.includes('heart-beat') && !msg.includes('PONG')) {
            // eslint-disable-next-line no-console
            console.debug('[WS]', msg);
          }
        }
      },
    });

    client.onConnect = () => {
      setIsConnected(true);

      client.subscribe('/user/queue/notifications', (message) => {
        try {
          const notification: Notification = JSON.parse(message.body);
          handleIncomingNotification(notification);
        } catch (e) {
          console.error('[WS] Failed to parse notification:', e);
        }
      });

      client.subscribe('/user/queue/unread-count', (message) => {
        try {
          const payload = JSON.parse(message.body);
          handleUnreadCountUpdate(payload);
        } catch (e) {
          console.error('[WS] Failed to parse unread count:', e);
        }
      });
    };

    client.onDisconnect = () => {
      setIsConnected(false);
    };

    client.onStompError = (frame) => {
      console.error('[WS] STOMP error:', frame.headers['message'], frame.body);
      setIsConnected(false);
    };

    client.onWebSocketError = (event) => {
      console.error('[WS] WebSocket error:', event);
      setIsConnected(false);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setIsConnected(false);
    };
  }, [userId, handleIncomingNotification, handleUnreadCountUpdate]);

  return { isConnected };
}
