import { Wallet, RefreshCw } from 'lucide-react';
import { motion } from 'framer-motion';

import { useWallet } from '../../hooks';
import { formatCurrency } from './utils';

interface WalletCardProps {
  userId: string;
}

/**
 * Displays the user's wallet balance with loading / error / empty states.
 * Owns its own data via `useWallet` — no props needed beyond the userId.
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
        <button
          onClick={() => refetch()}
          disabled={isLoading}
          className="p-2 text-white/70 hover:text-white hover:bg-white/10 rounded-full transition-colors disabled:opacity-50"
          title="Refresh balance"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
        </button>
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
            <p className="text-blue-200 text-sm">
              {formatCurrency(wallet.reservedBalance)} reserved for pending transfers
            </p>
          )}
        </>
      ) : (
        <p className="text-2xl font-bold mb-2">--</p>
      )}
    </motion.div>
  );
}
