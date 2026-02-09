import api from './axios';

export type NotificationType =
  | 'USER_REGISTRATION'
  | 'EMAIL_VERIFICATION'
  | 'PASSWORD_RESET'
  | 'PAYMENT_INITIATED'
  | 'PAYMENT_COMPLETED'
  | 'PAYMENT_FAILED'
  | 'PAYMENT_REFUNDED'
  | 'BILL_PAYMENT_INITIATED'
  | 'BILL_PAYMENT_COMPLETED'
  | 'BILL_PAYMENT_FAILED'
  | 'TRANSFER_SENT'
  | 'TRANSFER_RECEIVED'
  | 'ACCOUNT_UPDATE'
  | 'SECURITY_ALERT'
  | 'PROMOTIONAL'
  | 'SYSTEM';

export type NotificationChannel = 'EMAIL' | 'SMS' | 'PUSH' | 'IN_APP';

export type NotificationStatus =
  | 'PENDING'
  | 'SENDING'
  | 'SENT'
  | 'DELIVERED'
  | 'FAILED'
  | 'READ';

export interface Notification {
  id: string;
  userId: string;
  type: NotificationType;
  channel: NotificationChannel;
  subject: string;
  content: string;
  recipient: string | null;
  status: NotificationStatus;
  errorMessage: string | null;
  sentAt: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationPreferences {
  id: string;
  userId: string;
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  inAppEnabled: boolean;
  paymentNotifications: boolean;
  securityNotifications: boolean;
  promotionalNotifications: boolean;
  systemNotifications: boolean;
}

export interface NotificationPreferencesRequest {
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  inAppEnabled: boolean;
  paymentNotifications: boolean;
  securityNotifications: boolean;
  promotionalNotifications: boolean;
  systemNotifications: boolean;
}

export interface NotificationPage {
  content: Notification[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const notificationService = {
  getByUserId: (userId: string) =>
    api.get<Notification[]>(`/api/v1/notifications/user/${userId}`).then(r => r.data),

  getByUserIdPaged: (userId: string, page = 0, size = 20) =>
    api
      .get<NotificationPage>(`/api/v1/notifications/user/${userId}/paged`, {
        params: { page, size, sort: 'createdAt,desc' },
      })
      .then(r => r.data),

  getUnread: (userId: string) =>
    api.get<Notification[]>(`/api/v1/notifications/user/${userId}/unread`).then(r => r.data),

  getUnreadCount: (userId: string) =>
    api
      .get<{ count: number }>(`/api/v1/notifications/user/${userId}/unread/count`)
      .then(r => r.data.count),

  markAsRead: (notificationId: string) =>
    api.post<Notification>(`/api/v1/notifications/${notificationId}/read`).then(r => r.data),

  markAllAsRead: (userId: string) =>
    api.post<void>(`/api/v1/notifications/user/${userId}/read-all`),

  delete: (notificationId: string) =>
    api.delete<void>(`/api/v1/notifications/${notificationId}`),

  getPreferences: (userId: string) =>
    api
      .get<NotificationPreferences>(`/api/v1/notifications/user/${userId}/preferences`)
      .then(r => r.data),

  updatePreferences: (userId: string, data: NotificationPreferencesRequest) =>
    api
      .put<NotificationPreferences>(`/api/v1/notifications/user/${userId}/preferences`, data)
      .then(r => r.data),
};
