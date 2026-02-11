import { motion } from 'framer-motion';
import {
  CreditCard,
  Zap,
  Droplets,
  Wifi,
  Smartphone,
  Flame,
  Shield,
  Home,
  Tv,
  Building2,
  GraduationCap,
  FileText,
  type LucideIcon,
} from 'lucide-react';

import type { BillCategory } from '../../api/billPaymentApi';
import type { TransactionType } from '../../api/transactionDetailApi';
import { BILL_CATEGORY_LABELS } from '../../api/billPaymentApi';
import { useBillPayments } from '../../hooks';
import { formatCurrency, statusBadgeClasses } from './utils';
import SectionSkeleton from './SectionSkeleton';
import SectionError from './SectionError';
import EmptyState from './EmptyState';

const CATEGORY_ICONS: Record<BillCategory, LucideIcon> = {
  ELECTRICITY: Zap,
  WATER: Droplets,
  INTERNET: Wifi,
  PHONE: Smartphone,
  GAS: Flame,
  INSURANCE: Shield,
  RENT: Home,
  SUBSCRIPTION: Tv,
  GOVERNMENT: Building2,
  EDUCATION: GraduationCap,
  OTHER: FileText,
};

const CATEGORY_COLORS: Record<BillCategory, string> = {
  ELECTRICITY: 'bg-yellow-500/20 text-yellow-400',
  WATER: 'bg-blue-500/20 text-blue-400',
  INTERNET: 'bg-cyan-500/20 text-cyan-400',
  PHONE: 'bg-green-500/20 text-green-400',
  GAS: 'bg-orange-500/20 text-orange-400',
  INSURANCE: 'bg-indigo-500/20 text-indigo-400',
  RENT: 'bg-pink-500/20 text-pink-400',
  SUBSCRIPTION: 'bg-purple-500/20 text-purple-400',
  GOVERNMENT: 'bg-slate-500/20 text-slate-400',
  EDUCATION: 'bg-teal-500/20 text-teal-400',
  OTHER: 'bg-gray-500/20 text-gray-400',
};

interface RecentBillPaymentsProps {
  userId: string;
  onTransactionSelect?: (type: TransactionType, id: string) => void;
}

/**
 * Self-contained recent bill payments list.
 * Owns its own query via `useBillPayments` - handles loading, error, and empty states.
 */
export default function RecentBillPayments({ userId, onTransactionSelect }: RecentBillPaymentsProps) {
  const { data, isLoading, isError, refetch } = useBillPayments(userId, 0, 5);

  if (isLoading) return <SectionSkeleton rows={3} />;
  if (isError) return <SectionError message="Failed to load bill payments" onRetry={refetch} />;

  const bills = data?.content ?? [];

  if (bills.length === 0) {
    return (
      <EmptyState
        icon={<CreditCard className="w-6 h-6 text-dark-500" />}
        title="No bill payments yet"
        description="Pay a bill to see your payment history here."
      />
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.27 }}
      className="card p-6 mb-8"
    >
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-white">Recent Bill Payments</h2>
        <span className="text-xs text-dark-400">
          {data?.totalElements ?? 0} total
        </span>
      </div>

      <div className="space-y-3">
        {bills.map((bill) => {
          const CategoryIcon = CATEGORY_ICONS[bill.category] ?? FileText;
          const colorClasses = CATEGORY_COLORS[bill.category] ?? 'bg-gray-500/20 text-gray-400';
          return (
            <div
              key={bill.id}
              onClick={() => onTransactionSelect?.('BILL_PAYMENT', bill.id)}
              className="flex items-center justify-between p-3 bg-dark-800/50 rounded-lg border border-dark-700/50 cursor-pointer hover:bg-dark-800 hover:border-dark-600 transition-colors"
            >
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center ${colorClasses.split(' ')[0]}`}
                >
                  <CategoryIcon className={`w-5 h-5 ${colorClasses.split(' ')[1]}`} />
                </div>
                <div>
                  <p className="font-medium text-white">{bill.billerName}</p>
                  <p className="text-sm text-dark-400">
                    {BILL_CATEGORY_LABELS[bill.category]}
                    {bill.description ? ` — ${bill.description}` : ''}
                  </p>
                </div>
              </div>
              <div className="text-right">
                <p className="font-semibold text-red-400">
                  -{formatCurrency(bill.totalAmount)}
                </p>
                <p
                  className={`text-xs font-medium px-2 py-0.5 rounded-full inline-block ${statusBadgeClasses(bill.status)}`}
                >
                  {bill.status}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </motion.div>
  );
}
