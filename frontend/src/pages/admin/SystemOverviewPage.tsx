import { motion } from 'framer-motion';

import { useAdminDashboardMetrics, useAdminTransactionMetrics, useAdminWalletMetrics } from '../../hooks/useAdmin';

function MetricCard({ label, value, sub, color }: { label: string; value: string | number; sub?: string; color: string }) {
  return (
    <div className="bg-dark-800 border border-dark-700 rounded-xl p-5">
      <p className="text-dark-400 text-sm">{label}</p>
      <p className={`text-2xl font-bold mt-1 ${color}`}>{value}</p>
      {sub && <p className="text-dark-500 text-xs mt-1">{sub}</p>}
    </div>
  );
}

function ServiceCard({ name, status }: { name: string; status: 'UP' | 'DOWN' | 'DEGRADED' }) {
  const colors = { UP: 'bg-green-500', DOWN: 'bg-red-500', DEGRADED: 'bg-yellow-500' };
  return (
    <div className="bg-dark-800 border border-dark-700 rounded-xl p-5 flex items-center gap-4">
      <span className={`h-3 w-3 rounded-full ${colors[status]}`} />
      <div>
        <p className="text-white font-medium">{name}</p>
        <p className="text-dark-500 text-xs uppercase tracking-wide">{status}</p>
      </div>
    </div>
  );
}

export default function SystemOverviewPage() {
  const { data: userMetrics, isLoading: loadingUsers } = useAdminDashboardMetrics();
  const { data: txMetrics, isLoading: loadingTx } = useAdminTransactionMetrics();
  const { data: walletMetrics, isLoading: loadingWallets } = useAdminWalletMetrics();

  const isLoading = loadingUsers || loadingTx || loadingWallets;

  // Service discovery - in production, wire to Eureka/actuator
  const services = [
    { name: 'API Gateway', status: 'UP' as const },
    { name: 'User Service', status: 'UP' as const },
    { name: 'Payment Service', status: 'UP' as const },
    { name: 'Wallet Service', status: 'UP' as const },
    { name: 'Auth Service', status: 'UP' as const },
    { name: 'Notification Service', status: 'UP' as const },
    { name: 'Service Registry (Eureka)', status: 'UP' as const },
  ];

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-white">System Overview</h1>
        <div className="animate-pulse space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="bg-dark-800 border border-dark-700 rounded-xl h-24" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-bold text-white">System Overview</h1>
        <p className="text-dark-400 mt-1">Platform health, service status, and real-time metrics</p>
      </motion.div>

      {/* Service Health */}
      <section>
        <h2 className="text-lg font-semibold text-dark-200 mb-3">Service Health</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {services.map((s) => (
            <ServiceCard key={s.name} {...s} />
          ))}
        </div>
      </section>

      {/* User Metrics Section */}
      <section>
        <h2 className="text-lg font-semibold text-dark-200 mb-3">User Metrics</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <MetricCard label="Total Users" value={userMetrics?.totalUsers ?? 0} color="text-white" />
          <MetricCard label="Active" value={userMetrics?.activeUsers ?? 0} color="text-green-400" />
          <MetricCard label="Suspended" value={userMetrics?.suspendedUsers ?? 0} color="text-red-400" />
          <MetricCard label="Pending Verification" value={userMetrics?.pendingVerification ?? 0} color="text-yellow-400" />
          <MetricCard label="Admins" value={userMetrics?.adminCount ?? 0} color="text-purple-400" />
          <MetricCard label="Merchants" value={userMetrics?.merchantCount ?? 0} color="text-blue-400" />
          <MetricCard label="Regular Users" value={userMetrics?.regularUserCount ?? 0} color="text-gray-300" />
          <MetricCard label="Recent Audit Actions" value={userMetrics?.recentAuditActions24h ?? 0} sub="Last 24 hours" color="text-orange-400" />
        </div>
      </section>

      {/* Transaction Metrics */}
      <section>
        <h2 className="text-lg font-semibold text-dark-200 mb-3">Transaction Volume</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <MetricCard label="Total Transfers" value={txMetrics?.totalTransfers ?? 0} color="text-white" />
          <MetricCard label="Completed Transfers" value={txMetrics?.completedTransfers ?? 0} color="text-green-400" />
          <MetricCard label="Pending Transfers" value={txMetrics?.pendingTransfers ?? 0} color="text-yellow-400" />
          <MetricCard label="Failed Transfers" value={txMetrics?.failedTransfers ?? 0} color="text-red-400" />
          <MetricCard label="Total Bill Payments" value={txMetrics?.totalBillPayments ?? 0} color="text-white" />
          <MetricCard label="Completed Bills" value={txMetrics?.completedBillPayments ?? 0} color="text-green-400" />
          <MetricCard label="Total Money Requests" value={txMetrics?.totalMoneyRequests ?? 0} color="text-white" />
          <MetricCard label="Pending Requests" value={txMetrics?.pendingMoneyRequests ?? 0} color="text-yellow-400" />
        </div>
      </section>

      {/* Wallet Metrics */}
      <section>
        <h2 className="text-lg font-semibold text-dark-200 mb-3">Wallet Metrics</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <MetricCard label="Total Wallets" value={walletMetrics?.totalWallets ?? 0} color="text-white" />
          <MetricCard label="Active Wallets" value={walletMetrics?.activeWallets ?? 0} color="text-green-400" />
          <MetricCard label="Frozen Wallets" value={walletMetrics?.frozenWallets ?? 0} color="text-blue-400" />
          <MetricCard label="Closed Wallets" value={walletMetrics?.closedWallets ?? 0} color="text-gray-500" />
          <MetricCard
            label="Total Balance (All)"
            value={`$${(walletMetrics?.totalBalance ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
            color="text-green-300"
          />

        </div>
      </section>

      {/* Quick Facts */}
      <section className="bg-dark-800 border border-dark-700 rounded-xl p-6">
        <h2 className="text-lg font-semibold text-dark-200 mb-3">Platform Summary</h2>
        <ul className="space-y-2 text-sm text-dark-400 list-disc list-inside">
          <li>{services.filter((s) => s.status === 'UP').length} / {services.length} services healthy</li>
          <li>{userMetrics?.totalUsers ?? 0} registered users, {userMetrics?.activeUsers ?? 0} active</li>
          <li>{(txMetrics?.totalTransfers ?? 0) + (txMetrics?.totalBillPayments ?? 0) + (txMetrics?.totalMoneyRequests ?? 0)} total transactions processed</li>
          <li>${(walletMetrics?.totalBalance ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })} in total wallet balances</li>
        </ul>
      </section>
    </div>
  );
}
