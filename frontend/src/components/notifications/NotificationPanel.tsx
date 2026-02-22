import { useMemo } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowUpRight,
  ArrowDownLeft,
  CreditCard,
  Receipt,
  ShieldAlert,
  UserPlus,
  Mail,
  Bell,
  Megaphone,
  ServerCrash,
  Sparkles,
  CheckCheck,
  Loader2,
  BellOff,
} from 'lucide-react';
import { useNotifications, useMarkAsRead, useMarkAllAsRead } from '../../hooks/useNotifications';
import type { Notification, NotificationType } from '../../api/notificationApi';

function getNotificationIcon(type: NotificationType) {
  switch (type) {
    case 'TRANSFER_SENT':
      return { Icon: ArrowUpRight, color: 'text-orange-400', bg: 'bg-orange-500/20' };
    case 'TRANSFER_RECEIVED':
      return { Icon: ArrowDownLeft, color: 'text-green-400', bg: 'bg-green-500/20' };
    case 'PAYMENT_COMPLETED':
    case 'PAYMENT_INITIATED':
      return { Icon: CreditCard, color: 'text-primary-400', bg: 'bg-primary-500/20' };
    case 'PAYMENT_FAILED':
    case 'PAYMENT_REFUNDED':
      return { Icon: CreditCard, color: 'text-red-400', bg: 'bg-red-500/20' };
    case 'BILL_PAYMENT_COMPLETED':
    case 'BILL_PAYMENT_INITIATED':
      return { Icon: Receipt, color: 'text-blue-400', bg: 'bg-blue-500/20' };
    case 'BILL_PAYMENT_FAILED':
      return { Icon: Receipt, color: 'text-red-400', bg: 'bg-red-500/20' };
    case 'SECURITY_ALERT':
      return { Icon: ShieldAlert, color: 'text-yellow-400', bg: 'bg-yellow-500/20' };
    case 'USER_REGISTRATION':
      return { Icon: UserPlus, color: 'text-green-400', bg: 'bg-green-500/20' };
    case 'EMAIL_VERIFICATION':
    case 'PASSWORD_RESET':
      return { Icon: Mail, color: 'text-blue-400', bg: 'bg-blue-500/20' };
    case 'PROMOTIONAL':
      return { Icon: Megaphone, color: 'text-purple-400', bg: 'bg-purple-500/20' };
    case 'SYSTEM':
      return { Icon: ServerCrash, color: 'text-dark-300', bg: 'bg-dark-600/50' };
    case 'PLAN_UPGRADED':
      return { Icon: Sparkles, color: 'text-amber-400', bg: 'bg-amber-500/20' };
    default:
      return { Icon: Bell, color: 'text-primary-400', bg: 'bg-primary-500/20' };
  }
}

function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  if (diffHour < 24) return `${diffHour}h ago`;
  if (diffDay < 7) return `${diffDay}d ago`;
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function groupByDate(notifications: Notification[]) {
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  const groups: { label: string; items: Notification[] }[] = [];
  const todayItems: Notification[] = [];
  const yesterdayItems: Notification[] = [];
  const olderItems: Notification[] = [];

  for (const n of notifications) {
    const date = new Date(n.createdAt);
    if (date.toDateString() === today.toDateString()) {
      todayItems.push(n);
    } else if (date.toDateString() === yesterday.toDateString()) {
      yesterdayItems.push(n);
    } else {
      olderItems.push(n);
    }
  }

  if (todayItems.length > 0) groups.push({ label: 'Today', items: todayItems });
  if (yesterdayItems.length > 0) groups.push({ label: 'Yesterday', items: yesterdayItems });
  if (olderItems.length > 0) groups.push({ label: 'Earlier', items: olderItems });

  return groups;
}

interface NotificationItemProps {
  notification: Notification;
  onMarkAsRead: (id: string) => void;
  isMarkingRead: boolean;
}

function NotificationItem({ notification, onMarkAsRead, isMarkingRead }: NotificationItemProps) {
  const { Icon, color, bg } = getNotificationIcon(notification.type);
  const isUnread = !notification.readAt;

  return (
    <button
      onClick={() => isUnread && !isMarkingRead && onMarkAsRead(notification.id)}
      className={`w-full text-left px-4 py-3 flex items-start gap-3 hover:bg-white/[0.03] ${
        isUnread ? 'cursor-pointer' : 'cursor-default'
      }`}
      disabled={isMarkingRead}
    >
      <div className={`p-2 rounded-lg ${bg} flex-shrink-0 mt-0.5`}>
        <Icon className={`w-4 h-4 ${color}`} />
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <p
            className={`text-sm truncate ${
              isUnread ? 'font-semibold text-white' : 'font-normal text-dark-300'
            }`}
          >
            {notification.subject}
          </p>
          {isUnread && (
            <span className="w-2 h-2 rounded-full bg-primary-500 flex-shrink-0" />
          )}
        </div>
        <p className="text-xs text-dark-400 mt-0.5 line-clamp-2">{notification.content}</p>
        <p className="text-[10px] text-dark-500 mt-1">{formatRelativeTime(notification.createdAt)}</p>
      </div>
    </button>
  );
}

interface NotificationPanelProps {
  userId: string;
  onClose: () => void;
}

export default function NotificationPanel({ userId, onClose }: NotificationPanelProps) {
  const { data: notifications, isLoading, isError } = useNotifications(userId);
  const markAsRead = useMarkAsRead(userId);
  const markAllAsRead = useMarkAllAsRead(userId);

  const groups = useMemo(
    () => groupByDate(notifications ?? []),
    [notifications],
  );

  const hasUnread = notifications?.some((n) => !n.readAt) ?? false;

  return (
    <div className="w-96 max-h-[520px] bg-dark-900/95 backdrop-blur-xl border border-dark-700/50 rounded-xl shadow-2xl shadow-black/50 flex flex-col overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b border-dark-800/50">
        <h3 className="text-sm font-semibold text-white">Notifications</h3>
        {hasUnread && (
          <button
            onClick={() => markAllAsRead.mutate()}
            disabled={markAllAsRead.isPending}
            className="flex items-center gap-1.5 text-xs text-primary-400 hover:text-primary-300 transition-colors disabled:opacity-50"
          >
            {markAllAsRead.isPending ? (
              <Loader2 className="w-3 h-3 animate-spin" />
            ) : (
              <CheckCheck className="w-3 h-3" />
            )}
            Mark all read
          </button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto scrollbar-thin scrollbar-thumb-dark-700 scrollbar-track-transparent">
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="w-6 h-6 text-primary-500 animate-spin" />
          </div>
        )}

        {isError && (
          <div className="px-4 py-8 text-center">
            <p className="text-sm text-red-400">Failed to load notifications</p>
          </div>
        )}

        {!isLoading && !isError && groups.length === 0 && (
          <div className="flex flex-col items-center justify-center py-12 px-4">
            <div className="p-3 bg-dark-800/50 rounded-xl mb-3">
              <BellOff className="w-6 h-6 text-dark-500" />
            </div>
            <p className="text-sm text-dark-400">No notifications yet</p>
            <p className="text-xs text-dark-500 mt-1">
              We'll notify you when something happens
            </p>
          </div>
        )}

        {groups.map((group) => (
          <div key={group.label}>
            <div className="px-4 py-2 sticky top-0 bg-dark-900/95 backdrop-blur-xl z-10">
              <p className="text-[10px] uppercase tracking-wider font-semibold text-dark-500">
                {group.label}
              </p>
            </div>
            {group.items.map((notification, index) => (
              <motion.div
                key={notification.id}
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.03 }}
              >
                <NotificationItem
                  notification={notification}
                  onMarkAsRead={(id) => markAsRead.mutate(id)}
                  isMarkingRead={markAsRead.isPending}
                />
              </motion.div>
            ))}
          </div>
        ))}
      </div>

      {(notifications?.length ?? 0) > 0 && (
        <div className="border-t border-dark-800/50 px-4 py-2.5">
          <button
            onClick={onClose}
            className="w-full text-center text-xs text-dark-400 hover:text-primary-400 transition-colors"
          >
            Close
          </button>
        </div>
      )}
    </div>
  );
}
