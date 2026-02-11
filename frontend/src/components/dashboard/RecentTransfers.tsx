import { motion } from 'framer-motion';
import { ArrowUpRight, ArrowDownLeft, ArrowLeftRight } from 'lucide-react';

import type { TransactionType } from '../../api/transactionDetailApi';
import { useTransferHistory } from '../../hooks';
import { formatCurrency, statusBadgeClasses } from './utils';
import SectionSkeleton from './SectionSkeleton';
import SectionError from './SectionError';
import EmptyState from './EmptyState';

interface RecentTransfersProps {
  userId: string;
  onTransactionSelect?: (type: TransactionType, id: string) => void;
}

/**
 * Self-contained recent transfers list.
 * Owns its own query via `useTransferHistory` - handles loading, error, and empty states.
 */
export default function RecentTransfers({ userId, onTransactionSelect }: RecentTransfersProps) {
  const { data, isLoading, isError, refetch } = useTransferHistory(userId);

  if (isLoading) return <SectionSkeleton rows={3} />;
  if (isError) return <SectionError message="Failed to load transfers" onRetry={refetch} />;

  const transfers = data?.content ?? [];

  if (transfers.length === 0) {
    return (
      <EmptyState
        icon={<ArrowLeftRight className="w-6 h-6 text-dark-500" />}
        title="No transfers yet"
        description="Send or request money to see your transfer history here."
      />
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.25 }}
      className="card p-6 mb-8"
    >
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-white">Recent Transfers</h2>
        <span className="text-xs text-dark-400">
          {data?.totalElements ?? 0} total
        </span>
      </div>

      <div className="space-y-3">
        {transfers.map((transfer) => {
          const isSent = transfer.senderUserId === userId;
          return (
            <div
              key={transfer.id}
              onClick={() => onTransactionSelect?.('TRANSFER', transfer.id)}
              className="flex items-center justify-between p-3 bg-dark-800/50 rounded-lg border border-dark-700/50 cursor-pointer hover:bg-dark-800 hover:border-dark-600 transition-colors"
            >
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center ${
                    isSent ? 'bg-red-500/20' : 'bg-secondary-500/20'
                  }`}
                >
                  {isSent ? (
                    <ArrowUpRight className="w-5 h-5 text-red-400" />
                  ) : (
                    <ArrowDownLeft className="w-5 h-5 text-secondary-400" />
                  )}
                </div>
                <div>
                  <p className="font-medium text-white">
                    {isSent ? 'Sent' : 'Received'}
                  </p>
                  <p className="text-sm text-dark-400">
                    {transfer.description || transfer.transactionReference}
                  </p>
                </div>
              </div>
              <div className="text-right">
                <p
                  className={`font-semibold ${
                    isSent ? 'text-red-400' : 'text-secondary-400'
                  }`}
                >
                  {isSent ? '-' : '+'}
                  {formatCurrency(transfer.amount)}
                </p>
                <p
                  className={`text-xs font-medium px-2 py-0.5 rounded-full inline-block ${statusBadgeClasses(transfer.status)}`}
                >
                  {transfer.status}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </motion.div>
  );
}
