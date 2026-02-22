import { Wallet, RefreshCw, Zap, Sparkles, Building2, ArrowUpRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';

import { useWallet } from '../../hooks';
import { formatCurrency } from './utils';

const PLAN_META = {
  STARTER:    { label: 'Starter',    icon: Zap,       gradient: 'from-dark-500 to-dark-600',       text: 'text-dark-300',  bg: 'bg-dark-700/60' },
  PRO:        { label: 'Pro',        icon: Sparkles,  gradient: 'from-primary-500 to-primary-600', text: 'text-primary-300', bg: 'bg-primary-500/15' },
  ENTERPRISE: { label: 'Enterprise', icon: Building2,  gradient: 'from-secondary-500 to-secondary-600', text: 'text-secondary-300', bg: 'bg-secondary-500/15' },
} as const;

interface WalletCardProps {
  userId: string;
}

/**
 * Displays the user's wallet balance with loading / error / empty states.
 * Owns its own data via `useWallet` - no props needed beyond the userId.
 */
export default function WalletCard({ userId }: WalletCardProps) {
  const { data: wallet, isLoading, isError, refetch } = useWallet(userId);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1 }}
      className="bg-gradient-to-br from-primary-600 to-primary-500 rounded-2xl p-6 text-white mb-8 shadow-lg shadow-primary-500/25"
    >
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <Wallet className="w-6 h-6" />
          <span className="text-blue-100">Available Balance</span>
        </div>
        <div className="flex items-center gap-2">
          {wallet && (() => {
            const meta = PLAN_META[wallet.plan];
            const Icon = meta.icon;
            return (
              <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${meta.bg} ${meta.text} backdrop-blur-sm`}>
                <Icon className="w-3 h-3" />
                {meta.label}
              </span>
            );
          })()}
          <button
            onClick={() => refetch()}
            disabled={isLoading}
            className="p-2 text-white/70 hover:text-white hover:bg-white/10 rounded-full transition-colors disabled:opacity-50"
            title="Refresh balance"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="h-12 flex items-center">
          <div className="w-48 h-8 bg-white/20 rounded animate-pulse" />
        </div>
      ) : isError ? (
        <p className="text-blue-200 text-sm">Unable to load balance</p>
      ) : wallet ? (
        <>
          <p className="text-4xl font-bold mb-2">
            {formatCurrency(wallet.availableBalance)}
          </p>
          {wallet.reservedBalance > 0 && (
            <p className="text-blue-200 text-sm mb-3">
              {formatCurrency(wallet.reservedBalance)} reserved for pending transfers
            </p>
          )}

          <div className="mt-4 pt-4 border-t border-white/10">
            <div className="flex items-center justify-between text-xs text-blue-200 mb-2">
              <span>Daily Limit</span>
              <span className="text-white font-medium">{formatCurrency(wallet.dailyTransactionLimit)}</span>
            </div>
            <div className="flex items-center justify-between text-xs text-blue-200">
              <span>Monthly Limit</span>
              <span className="text-white font-medium">{formatCurrency(wallet.monthlyTransactionLimit)}</span>
            </div>
            {wallet.plan !== 'ENTERPRISE' && (
              <Link
                to="/settings?tab=plan"
                className="mt-3 flex items-center justify-center gap-1.5 w-full py-2 bg-white/10 hover:bg-white/20 rounded-xl text-xs font-medium text-white transition-colors"
              >
                Upgrade Plan
                <ArrowUpRight className="w-3 h-3" />
              </Link>
            )}
          </div>
        </>
      ) : (
        <p className="text-2xl font-bold mb-2">--</p>
      )}
    </motion.div>
  );
}
