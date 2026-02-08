import { motion } from 'framer-motion';
import { User } from 'lucide-react';

import type { User as UserType } from '../../api/authApi';

interface AccountStatusProps {
  user: UserType;
}

/** Displays account verification & role info. Purely presentational. */
export default function AccountStatus({ user }: AccountStatusProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.3 }}
      className="card p-6"
    >
      <h2 className="text-lg font-semibold text-white mb-4">Account Status</h2>
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <User className="w-5 h-5 text-dark-400" />
            <span className="text-dark-300">Account Status</span>
          </div>
          <span
            className={`px-3 py-1 rounded-full text-sm font-medium ${
              user.status === 'ACTIVE'
                ? 'bg-secondary-500/20 text-secondary-400'
                : user.status === 'PENDING_VERIFICATION'
                  ? 'bg-yellow-500/20 text-yellow-400'
                  : 'bg-dark-700 text-dark-300'
            }`}
          >
            {user.status.replace('_', ' ')}
          </span>
        </div>

        <div className="flex items-center justify-between">
          <span className="text-dark-300">Email Verified</span>
          <span
            className={`px-3 py-1 rounded-full text-sm font-medium ${
              user.emailVerified
                ? 'bg-secondary-500/20 text-secondary-400'
                : 'bg-red-500/20 text-red-400'
            }`}
          >
            {user.emailVerified ? 'Verified' : 'Not Verified'}
          </span>
        </div>

        <div className="flex items-center justify-between">
          <span className="text-dark-300">Role</span>
          <span className="px-3 py-1 bg-primary-500/20 text-primary-400 rounded-full text-sm font-medium">
            {user.role}
          </span>
        </div>
      </div>
    </motion.div>
  );
}
