import { useState } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';

import { useAuth } from '../contexts/AuthContext';
import {
  DashboardHeader,
  WalletCard,
  QuickActions,
  RecentTransfers,
  RecentBillPayments,
  AccountStatus,
} from '../components/dashboard';
import {
  SendMoneyModal,
  RequestMoneyModal,
  PayBillModal,
  PendingRequestsPanel,
} from '../components/payments';
import { NotificationProvider } from '../components/notifications';

export default function DashboardPage() {
  const { user, logout } = useAuth();

  const [isSendMoneyOpen, setIsSendMoneyOpen] = useState(false);
  const [isRequestMoneyOpen, setIsRequestMoneyOpen] = useState(false);
  const [isPayBillOpen, setIsPayBillOpen] = useState(false);

  if (!user) return null;

  return (
    <NotificationProvider userId={user.id}>
    <div className="min-h-screen bg-dark-950">
      <DashboardHeader user={user} onLogout={logout} />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <h1 className="text-2xl font-bold text-white">
            Welcome back, {user.firstName}!
          </h1>
          <p className="text-dark-400">Here's your financial overview</p>
        </motion.div>

        {/* Each section owns its own data fetching */}
        <WalletCard userId={user.id} />

        <QuickActions
          onSendMoney={() => setIsSendMoneyOpen(true)}
          onRequestMoney={() => setIsRequestMoneyOpen(true)}
          onPayBills={() => setIsPayBillOpen(true)}
        />

        <PendingRequestsPanel userId={user.id} />

        <RecentTransfers userId={user.id} />

        <RecentBillPayments userId={user.id} />

        <AccountStatus user={user} />

        <div className="mt-8 text-center">
          <Link to="/" className="text-primary-400 hover:text-primary-300 text-sm">
            &larr; Back to Home
          </Link>
        </div>
      </main>

      {/* Modals - rendered at root level, only mount when needed */}
      <SendMoneyModal
        isOpen={isSendMoneyOpen}
        onClose={() => setIsSendMoneyOpen(false)}
        userId={user.id}
      />

      <RequestMoneyModal
        isOpen={isRequestMoneyOpen}
        onClose={() => setIsRequestMoneyOpen(false)}
        userId={user.id}
      />

      <PayBillModal
        isOpen={isPayBillOpen}
        onClose={() => setIsPayBillOpen(false)}
        userId={user.id}
      />
    </div>
    </NotificationProvider>
  );
}
