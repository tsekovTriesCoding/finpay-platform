import { useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
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
  X,
} from 'lucide-react';

import type { Toast } from './NotificationProvider';
import type { NotificationType } from '../../api/notificationApi';

function getNotificationMeta(type: NotificationType) {
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
    case 'ACCOUNT_UPDATE':
      return { Icon: Bell, color: 'text-primary-400', bg: 'bg-primary-500/20' };
    default:
      return { Icon: Bell, color: 'text-dark-400', bg: 'bg-dark-600/50' };
  }
}

interface NotificationToastProps {
  toast: Toast;
  onDismiss: (id: string) => void;
  duration: number;
}

function NotificationToast({ toast, onDismiss, duration }: NotificationToastProps) {
  const { notification } = toast;
  const { Icon, color, bg } = getNotificationMeta(notification.type);

  useEffect(() => {
    const timer = setTimeout(() => onDismiss(toast.id), duration);
    return () => clearTimeout(timer);
  }, [toast.id, onDismiss, duration]);

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: -20, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, x: 100, scale: 0.9 }}
      transition={{ type: 'spring', damping: 25, stiffness: 350 }}
      className="relative w-80 bg-dark-900/95 backdrop-blur-xl border border-dark-700/50 rounded-xl shadow-2xl shadow-black/40 overflow-hidden"
    >
      <motion.div
        className="absolute top-0 left-0 h-0.5 bg-gradient-to-r from-primary-500 to-primary-400"
        initial={{ width: '100%' }}
        animate={{ width: '0%' }}
        transition={{ duration: duration / 1000, ease: 'linear' }}
      />

      <div className="p-4 flex items-start gap-3">
        <div className={`p-2 rounded-lg ${bg} flex-shrink-0`}>
          <Icon className={`w-4 h-4 ${color}`} />
        </div>

        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-white truncate">{notification.subject}</p>
          <p className="text-xs text-dark-400 mt-0.5 line-clamp-2">{notification.content}</p>
        </div>

        <button
          onClick={() => onDismiss(toast.id)}
          className="p-1 text-dark-500 hover:text-dark-300 transition-colors flex-shrink-0"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </motion.div>
  );
}

interface NotificationToastContainerProps {
  toasts: Toast[];
  onDismiss: (id: string) => void;
  duration: number;
}

export function NotificationToastContainer({
  toasts,
  onDismiss,
  duration,
}: NotificationToastContainerProps) {
  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2">
      <AnimatePresence mode="popLayout">
        {toasts.map((toast) => (
          <NotificationToast
            key={toast.id}
            toast={toast}
            onDismiss={onDismiss}
            duration={duration}
          />
        ))}
      </AnimatePresence>
    </div>
  );
}
