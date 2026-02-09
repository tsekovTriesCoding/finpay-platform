import { createContext, useContext } from 'react';

export interface NotificationContextValue {
  isConnected: boolean;
}

export const NotificationContext = createContext<NotificationContextValue>({
  isConnected: false,
});

export function useNotificationContext() {
  return useContext(NotificationContext);
}
