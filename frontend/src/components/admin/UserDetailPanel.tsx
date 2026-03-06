import { useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  Shield, UserX, UserCheck, RotateCcw, X,
  Mail, Calendar, CheckCircle2, XCircle, CreditCard, Clock,
  AlertTriangle,
} from 'lucide-react';

import { useChangeUserRole, useSuspendUser, useUnsuspendUser, useForcePasswordReset } from '../../hooks/useAdmin';
import type { AdminUser } from '../../api/adminApi';
import type { ConfirmAction } from './ConfirmModal';
import { STATUS_COLORS, ROLE_COLORS, ROLE_ICONS } from './constants';
import { formatDate } from '../../utils/exportUtils';

/* Small helper rendered inside the info section */
function InfoRow({
  icon: Icon,
  label,
  value,
  valueClass = 'text-gray-200',
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  valueClass?: string;
}) {
  return (
    <div className="flex items-center justify-between text-sm">
      <div className="flex items-center gap-2 text-gray-500">
        <Icon className="w-4 h-4" />
        {label}
      </div>
      <span className={valueClass}>{value}</span>
    </div>
  );
}

/* Slide-over user detail panel */
export default function UserDetailPanel({
  user,
  onClose,
  onAction,
}: {
  user: AdminUser;
  onClose: () => void;
  onAction: (action: ConfirmAction) => void;
}) {
  const changeRole = useChangeUserRole();
  const suspendUser = useSuspendUser();
  const unsuspendUser = useUnsuspendUser();
  const forceReset = useForcePasswordReset();

  const RoleIcon = ROLE_ICONS[user.role] ?? Shield;

  // Close on Escape key
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  /* Action builders */
  const requestRoleChange = (targetRole: 'ADMIN' | 'USER' | 'MERCHANT') => {
    const labels: Record<string, string> = { ADMIN: 'Admin', USER: 'User', MERCHANT: 'Merchant' };
    const colors: Record<string, string> = {
      ADMIN: 'bg-purple-600 hover:bg-purple-500',
      USER: 'bg-blue-600 hover:bg-blue-500',
      MERCHANT: 'bg-emerald-600 hover:bg-emerald-500',
    };
    const iconColors: Record<string, string> = {
      ADMIN: 'text-purple-400 bg-purple-500/20',
      USER: 'text-blue-400 bg-blue-500/20',
      MERCHANT: 'text-emerald-400 bg-emerald-500/20',
    };
    onAction({
      label: `Change Role to ${labels[targetRole]}`,
      description: `${user.email} will be assigned the ${labels[targetRole]} role. This takes effect immediately.`,
      icon: ROLE_ICONS[targetRole] ?? Shield,
      iconColor: iconColors[targetRole],
      btnColor: colors[targetRole],
      onConfirm: ({ onSuccess, onError }) =>
        changeRole.mutate({ userId: user.id, role: targetRole }, { onSuccess, onError }),
    });
  };

  const requestSuspend = () =>
    onAction({
      label: 'Suspend User',
      description: `${user.email} will be suspended and immediately lose access to the platform.`,
      icon: UserX,
      iconColor: 'text-red-400 bg-red-500/20',
      btnColor: 'bg-red-600 hover:bg-red-500',
      onConfirm: ({ onSuccess, onError }) =>
        suspendUser.mutate({ userId: user.id }, { onSuccess, onError }),
    });

  const requestUnsuspend = () =>
    onAction({
      label: 'Unsuspend User',
      description: `${user.email} will regain access to the platform.`,
      icon: UserCheck,
      iconColor: 'text-green-400 bg-green-500/20',
      btnColor: 'bg-green-600 hover:bg-green-500',
      onConfirm: ({ onSuccess, onError }) =>
        unsuspendUser.mutate(user.id, { onSuccess, onError }),
    });

  const requestForceReset = () =>
    onAction({
      label: 'Force Password Reset',
      description: `${user.email} will be required to change their password on next login.`,
      icon: RotateCcw,
      iconColor: 'text-yellow-400 bg-yellow-500/20',
      btnColor: 'bg-yellow-600 hover:bg-yellow-500',
      onConfirm: ({ onSuccess, onError }) =>
        forceReset.mutate(user.id, { onSuccess, onError }),
    });

  const availableRoles = (['ADMIN', 'USER', 'MERCHANT'] as const).filter((r) => r !== user.role);

  /* Render */
  return (
    <>
      {/* Backdrop */}
      <motion.div
        className="fixed inset-0 z-40 bg-black/40 backdrop-blur-sm"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
      />

      {/* Panel */}
      <motion.aside
        className="fixed right-0 top-0 z-50 h-full w-full max-w-md bg-dark-900 border-l border-dark-700 shadow-2xl
                   flex flex-col overflow-y-auto"
        initial={{ x: '100%' }}
        animate={{ x: 0 }}
        exit={{ x: '100%' }}
        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-dark-700">
          <h2 className="text-lg font-semibold text-white">User Details</h2>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg hover:bg-dark-700 text-gray-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Profile section */}
        <div className="p-5 border-b border-dark-700">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-full bg-gradient-to-br from-primary-500 to-secondary-500
                            flex items-center justify-center text-white font-bold text-xl shrink-0">
              {user.firstName?.[0]?.toUpperCase() ?? user.email[0].toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="text-white font-semibold text-lg truncate">
                {user.firstName} {user.lastName}
              </p>
              <div className="flex items-center gap-1.5 text-gray-400 text-sm">
                <Mail className="w-3.5 h-3.5 shrink-0" />
                <span className="truncate">{user.email}</span>
              </div>
            </div>
          </div>

          {/* Badges row */}
          <div className="flex flex-wrap gap-2 mt-4">
            <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${ROLE_COLORS[user.role]}`}>
              <RoleIcon className="w-3.5 h-3.5" />
              {user.role}
            </span>
            <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${STATUS_COLORS[user.status]}`}>
              {user.status.replace('_', ' ')}
            </span>
            <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-dark-700 text-gray-300">
              <CreditCard className="w-3.5 h-3.5 inline mr-1 -mt-0.5" />
              {user.plan}
            </span>
          </div>
        </div>

        {/* Info fields */}
        <div className="p-5 border-b border-dark-700 space-y-3">
          <InfoRow icon={Calendar} label="Member since" value={formatDate(user.createdAt)} />
          <InfoRow icon={Clock} label="Last login" value={formatDate(user.lastLoginAt) || 'Never'} />
          <InfoRow
            icon={user.emailVerified ? CheckCircle2 : XCircle}
            label="Email verified"
            value={user.emailVerified ? 'Verified' : 'Not verified'}
            valueClass={user.emailVerified ? 'text-green-400' : 'text-red-400'}
          />
          {user.phoneNumber && <InfoRow icon={Mail} label="Phone" value={user.phoneNumber} />}
          {user.city && (
            <InfoRow icon={Calendar} label="Location" value={[user.city, user.country].filter(Boolean).join(', ')} />
          )}
        </div>

        {/* Actions */}
        <div className="p-5 space-y-2 flex-1">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Actions</p>

          {/* Role changes */}
          {availableRoles.map((targetRole) => {
            const labels: Record<string, string> = { ADMIN: 'Promote to Admin', USER: 'Demote to User', MERCHANT: 'Make Merchant' };
            const styles: Record<string, string> = {
              ADMIN: 'text-purple-400 hover:bg-purple-500/10',
              USER: 'text-blue-400 hover:bg-blue-500/10',
              MERCHANT: 'text-emerald-400 hover:bg-emerald-500/10',
            };
            const RI = ROLE_ICONS[targetRole] ?? Shield;
            return (
              <button
                key={targetRole}
                onClick={() => requestRoleChange(targetRole)}
                className={`flex items-center gap-3 w-full px-4 py-2.5 rounded-xl text-sm font-medium
                           transition-colors ${styles[targetRole]}`}
              >
                <RI className="w-4 h-4" />
                {labels[targetRole]}
              </button>
            );
          })}

          <div className="border-t border-dark-700 my-3" />

          {/* Suspend / Unsuspend */}
          {user.status !== 'SUSPENDED' ? (
            <button
              onClick={requestSuspend}
              className="flex items-center gap-3 w-full px-4 py-2.5 rounded-xl text-sm font-medium
                         text-red-400 hover:bg-red-500/10 transition-colors"
            >
              <UserX className="w-4 h-4" />
              Suspend User
            </button>
          ) : (
            <button
              onClick={requestUnsuspend}
              className="flex items-center gap-3 w-full px-4 py-2.5 rounded-xl text-sm font-medium
                         text-green-400 hover:bg-green-500/10 transition-colors"
            >
              <UserCheck className="w-4 h-4" />
              Unsuspend User
            </button>
          )}

          {/* Force password reset */}
          <button
            onClick={requestForceReset}
            className="flex items-center gap-3 w-full px-4 py-2.5 rounded-xl text-sm font-medium
                       text-yellow-400 hover:bg-yellow-500/10 transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            Force Password Reset
          </button>
        </div>

        {/* Warning footer for suspended users */}
        {user.status === 'SUSPENDED' && (
          <div className="p-4 mx-5 mb-5 rounded-xl bg-red-500/10 border border-red-500/20 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-red-400">Account Suspended</p>
              <p className="text-xs text-gray-400 mt-0.5">This user cannot access the platform.</p>
            </div>
          </div>
        )}
      </motion.aside>
    </>
  );
}
