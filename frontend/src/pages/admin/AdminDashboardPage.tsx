import { motion } from 'framer-motion';
import {
  Users,
  ArrowLeftRight,
  Wallet,
  ScrollText,
  TrendingUp,
  AlertTriangle,
  Shield,
  Clock,
  DollarSign,
  UserCheck,
} from 'lucide-react';

import { useAdminDashboardMetrics, useAdminTransactionMetrics, useAdminWalletMetrics } from '../../hooks/useAdmin';
import KPICard from '../../components/admin/KPICard';
import { formatCurrency } from '../../utils/exportUtils';

export default function AdminDashboardPage() {
  const { data: userMetrics, isLoading: loadingUsers } = useAdminDashboardMetrics();
  const { data: txMetrics, isLoading: loadingTx } = useAdminTransactionMetrics();
  const { data: walletMetrics, isLoading: loadingWallets } = useAdminWalletMetrics();

  return (
    <div className="space-y-8">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-bold text-white">Admin Dashboard</h1>
        <p className="text-dark-400 mt-1">Platform overview and KPI metrics</p>
      </motion.div>

      {/* KPI Cards - Row 1: Users */}
      <div>
        <h2 className="text-sm font-semibold text-dark-400 uppercase tracking-wider mb-4">
          User Metrics
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <KPICard
            title="Total Users"
            value={userMetrics?.totalUsers}
            icon={Users}
            color="blue"
            loading={loadingUsers}
          />
          <KPICard
            title="Active Users"
            value={userMetrics?.activeUsers}
            icon={UserCheck}
            color="green"
            loading={loadingUsers}
          />
          <KPICard
            title="Suspended"
            value={userMetrics?.suspendedUsers}
            icon={AlertTriangle}
            color="red"
            loading={loadingUsers}
          />
          <KPICard
            title="Pending Verification"
            value={userMetrics?.pendingVerification}
            icon={Clock}
            color="yellow"
            loading={loadingUsers}
          />
        </div>
      </div>

      {/* KPI Cards - Row 2: Transactions */}
      <div>
        <h2 className="text-sm font-semibold text-dark-400 uppercase tracking-wider mb-4">
          Transaction Volume
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <KPICard
            title="Total Transfers"
            value={txMetrics?.totalTransfers}
            icon={ArrowLeftRight}
            color="blue"
            loading={loadingTx}
          />
          <KPICard
            title="Transfer Volume"
            value={txMetrics?.totalTransferVolume != null
              ? formatCurrency(txMetrics.totalTransferVolume) : undefined}
            icon={DollarSign}
            color="green"
            loading={loadingTx}
          />
          <KPICard
            title="Failed Transfers"
            value={txMetrics?.failedTransfers}
            icon={AlertTriangle}
            color="red"
            loading={loadingTx}
          />
          <KPICard
            title="Bill Payments"
            value={txMetrics?.totalBillPayments}
            icon={TrendingUp}
            color="purple"
            loading={loadingTx}
          />
        </div>
      </div>

      {/* KPI Cards - Row 3: Wallets & Audit */}
      <div>
        <h2 className="text-sm font-semibold text-dark-400 uppercase tracking-wider mb-4">
          Wallets & Activity
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <KPICard
            title="Total Wallets"
            value={walletMetrics?.totalWallets}
            icon={Wallet}
            color="blue"
            loading={loadingWallets}
          />
          <KPICard
            title="Frozen Wallets"
            value={walletMetrics?.frozenWallets}
            icon={Shield}
            color="red"
            loading={loadingWallets}
          />
          <KPICard
            title="Platform Balance"
            value={walletMetrics?.totalBalance != null
              ? formatCurrency(walletMetrics.totalBalance) : undefined}
            icon={DollarSign}
            color="green"
            loading={loadingWallets}
          />
          <KPICard
            title="Admin Actions (24h)"
            value={userMetrics?.recentAuditActions24h}
            icon={ScrollText}
            color="purple"
            loading={loadingUsers}
          />
        </div>
      </div>

      {/* Role Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-dark-900 rounded-xl border border-dark-700 p-6">
          <h3 className="text-lg font-semibold text-white mb-4">User Distribution by Role</h3>
          {loadingUsers ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-8 bg-dark-800 rounded animate-pulse" />
              ))}
            </div>
          ) : (
            <div className="space-y-3">
              <RoleBar label="Regular Users" count={userMetrics?.regularUserCount ?? 0}
                total={userMetrics?.totalUsers ?? 1} color="bg-blue-500" />
              <RoleBar label="Merchants" count={userMetrics?.merchantCount ?? 0}
                total={userMetrics?.totalUsers ?? 1} color="bg-purple-500" />
              <RoleBar label="Admins" count={userMetrics?.adminCount ?? 0}
                total={userMetrics?.totalUsers ?? 1} color="bg-emerald-500" />
            </div>
          )}
        </div>

        <div className="bg-dark-900 rounded-xl border border-dark-700 p-6">
          <h3 className="text-lg font-semibold text-white mb-4">Transaction Status Overview</h3>
          {loadingTx ? (
            <div className="space-y-3">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="h-8 bg-dark-800 rounded animate-pulse" />
              ))}
            </div>
          ) : (
            <div className="space-y-3">
              <StatusRow label="Completed" value={txMetrics?.completedTransfers ?? 0} color="text-green-400" />
              <StatusRow label="Pending" value={txMetrics?.pendingTransfers ?? 0} color="text-yellow-400" />
              <StatusRow label="Failed" value={txMetrics?.failedTransfers ?? 0} color="text-red-400" />
              <StatusRow label="Money Requests (Pending)" value={txMetrics?.pendingMoneyRequests ?? 0} color="text-blue-400" />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// Sub-components

function RoleBar({ label, count, total, color }: {
  label: string; count: number; total: number; color: string;
}) {
  const pct = total > 0 ? (count / total) * 100 : 0;
  return (
    <div>
      <div className="flex justify-between text-sm mb-1">
        <span className="text-dark-300">{label}</span>
        <span className="text-dark-400">{count} ({pct.toFixed(1)}%)</span>
      </div>
      <div className="w-full bg-dark-800 rounded-full h-2">
        <div className={`${color} h-2 rounded-full transition-all`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

function StatusRow({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div className="flex justify-between items-center py-2 border-b border-dark-700 last:border-0">
      <span className="text-dark-300">{label}</span>
      <span className={`font-semibold ${color}`}>{value.toLocaleString()}</span>
    </div>
  );
}
