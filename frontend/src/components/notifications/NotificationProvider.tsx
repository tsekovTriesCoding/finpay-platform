import { useState, useCallback, type ReactNode } from 'react';

import { useWebSocket } from '../../hooks/useWebSocket';
import { NotificationToastContainer } from './NotificationToast';
import { NotificationContext } from '../../contexts/NotificationContext';
import type { Notification } from '../../api/notificationApi';

export interface Toast {
  id: string;
  notification: Notification;
  addedAt: number;
}

const MAX_TOASTS = 4;
const TOAST_DURATION = 6000;

interface NotificationProviderProps {
  userId: string;
  children: ReactNode;
}

/**
 * Wraps the dashboard with:
 * - WebSocket connection for real-time notifications
 * - Toast notification display
 */
export function NotificationProvider({ userId, children }: NotificationProviderProps) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = useCallback((notification: Notification) => {
    setToasts((prev) => {
      const newToast: Toast = {
        id: notification.id,
        notification,
        addedAt: Date.now(),
      };
      const updated = [newToast, ...prev].slice(0, MAX_TOASTS);
      return updated;
    });
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const { isConnected } = useWebSocket({
    userId,
    onNotification: addToast,
  });

  return (
    <NotificationContext.Provider value={{ isConnected }}>
      {children}
      <NotificationToastContainer
        toasts={toasts}
        onDismiss={removeToast}
        duration={TOAST_DURATION}
      />
    </NotificationContext.Provider>
  );
}
