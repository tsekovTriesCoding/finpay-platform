import { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  Wallet, 
  ArrowUpRight, 
  ArrowDownLeft, 
  CreditCard,
  TrendingUp,
  Settings,
  LogOut,
  User,
  RefreshCw
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';

import { useAuth } from '../contexts/AuthContext';
import { MoneyTransfer } from '../api';
import { useWallet, useTransferHistory } from '../hooks';
import { SendMoneyModal } from '../components/payments';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isSendMoneyOpen, setIsSendMoneyOpen] = useState(false);

  const {
    data: wallet,
    isLoading: isLoadingWallet,
    refetch: refetchWallet,
  } = useWallet(user?.id);

  const {
    data: transfersData,
    refetch: refetchTransfers,
  } = useTransferHistory(user?.id);

  const recentTransfers = transfersData?.content ?? [];

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const handleSendMoney = () => {
    setIsSendMoneyOpen(true);
  };

  const handleTransferComplete = (_transfer: MoneyTransfer) => {
    refetchWallet();
    refetchTransfers();
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const quickActions = [
    { icon: ArrowUpRight, label: 'Send Money', color: 'bg-blue-500', onClick: handleSendMoney },
    { icon: ArrowDownLeft, label: 'Request Money', color: 'bg-green-500', onClick: () => {} },
    { icon: CreditCard, label: 'Pay Bills', color: 'bg-purple-500', onClick: () => {} },
    { icon: TrendingUp, label: 'Investments', color: 'bg-orange-500', onClick: () => {} },
  ];

  return (
    <div className="min-h-screen bg-dark-950">
      <header className="bg-dark-900/50 border-b border-dark-800/50 backdrop-blur-xl">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-gradient-to-br from-primary-600 to-primary-500 rounded-lg flex items-center justify-center shadow-lg shadow-primary-500/25">
                <span className="text-white font-bold">F</span>
              </div>
              <span className="text-xl font-bold text-gradient">
                FinPay
              </span>
            </div>

            <div className="flex items-center gap-4">
              <button className="p-2 text-dark-400 hover:text-white transition-colors">
                <Settings className="w-5 h-5" />
              </button>
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-gradient-to-br from-primary-600 to-primary-500 rounded-full flex items-center justify-center text-white font-medium shadow-lg shadow-primary-500/25">
                  {user?.firstName?.[0]}{user?.lastName?.[0]}
                </div>
                <div className="hidden sm:block">
                  <p className="text-sm font-medium text-white">
                    {user?.firstName} {user?.lastName}
                  </p>
                  <p className="text-xs text-dark-400">{user?.email}</p>
                </div>
              </div>
              <button
                onClick={handleLogout}
                className="p-2 text-dark-400 hover:text-red-400 transition-colors"
                title="Logout"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <h1 className="text-2xl font-bold text-white">
            Welcome back, {user?.firstName}!
          </h1>
          <p className="text-dark-400">Here's your financial overview</p>
        </motion.div>

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
              onClick={() => refetchWallet()}
              disabled={isLoadingWallet}
              className="p-2 text-white/70 hover:text-white hover:bg-white/10 rounded-full transition-colors disabled:opacity-50"
              title="Refresh balance"
            >
              <RefreshCw className={`w-4 h-4 ${isLoadingWallet ? 'animate-spin' : ''}`} />
            </button>
          </div>
          {isLoadingWallet ? (
            <div className="h-12 flex items-center">
              <div className="w-48 h-8 bg-white/20 rounded animate-pulse" />
            </div>
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

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mb-8"
        >
          <h2 className="text-lg font-semibold text-white mb-4">Quick Actions</h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {quickActions.map((action, index) => (
              <button
                key={index}
                onClick={action.onClick}
                className="card p-4 hover:border-dark-700/50 transition-all hover:scale-[1.02] text-center"
              >
                <div className={`w-12 h-12 ${action.color} rounded-full flex items-center justify-center mx-auto mb-3 shadow-lg`}>
                  <action.icon className="w-6 h-6 text-white" />
                </div>
                <p className="text-sm font-medium text-white">{action.label}</p>
              </button>
            ))}
          </div>
        </motion.div>

        {recentTransfers.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.25 }}
            className="card p-6 mb-8"
          >
            <h2 className="text-lg font-semibold text-white mb-4">Recent Transfers</h2>
            <div className="space-y-3">
              {recentTransfers.map((transfer) => (
                <div 
                  key={transfer.id}
                  className="flex items-center justify-between p-3 bg-dark-800/50 rounded-lg border border-dark-700/50"
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                      transfer.senderUserId === user?.id 
                        ? 'bg-red-500/20' 
                        : 'bg-secondary-500/20'
                    }`}>
                      {transfer.senderUserId === user?.id ? (
                        <ArrowUpRight className="w-5 h-5 text-red-400" />
                      ) : (
                        <ArrowDownLeft className="w-5 h-5 text-secondary-400" />
                      )}
                    </div>
                    <div>
                      <p className="font-medium text-white">
                        {transfer.senderUserId === user?.id ? 'Sent' : 'Received'}
                      </p>
                      <p className="text-sm text-dark-400">
                        {transfer.description || transfer.transactionReference}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className={`font-semibold ${
                      transfer.senderUserId === user?.id 
                        ? 'text-red-400' 
                        : 'text-secondary-400'
                    }`}>
                      {transfer.senderUserId === user?.id ? '-' : '+'}
                      {formatCurrency(transfer.amount)}
                    </p>
                    <p className={`text-xs font-medium px-2 py-0.5 rounded-full inline-block ${
                      transfer.status === 'COMPLETED' 
                        ? 'bg-secondary-500/20 text-secondary-400'
                        : transfer.status === 'FAILED' || transfer.status === 'COMPENSATED'
                        ? 'bg-red-500/20 text-red-400'
                        : 'bg-yellow-500/20 text-yellow-400'
                    }`}>
                      {transfer.status}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        )}

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
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                user?.status === 'ACTIVE' 
                  ? 'bg-secondary-500/20 text-secondary-400'
                  : user?.status === 'PENDING_VERIFICATION'
                  ? 'bg-yellow-500/20 text-yellow-400'
                  : 'bg-dark-700 text-dark-300'
              }`}>
                {user?.status?.replace('_', ' ')}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-dark-300">Email Verified</span>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                user?.emailVerified 
                  ? 'bg-secondary-500/20 text-secondary-400'
                  : 'bg-red-500/20 text-red-400'
              }`}>
                {user?.emailVerified ? 'Verified' : 'Not Verified'}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-dark-300">Role</span>
              <span className="px-3 py-1 bg-primary-500/20 text-primary-400 rounded-full text-sm font-medium">
                {user?.role}
              </span>
            </div>
          </div>
        </motion.div>

        <div className="mt-8 text-center">
          <Link 
            to="/"
            className="text-primary-400 hover:text-primary-300 text-sm"
          >
            ← Back to Home
          </Link>
        </div>
      </main>

      {user?.id && (
        <SendMoneyModal
          isOpen={isSendMoneyOpen}
          onClose={() => setIsSendMoneyOpen(false)}
          userId={user.id}
          onTransferComplete={handleTransferComplete}
        />
      )}
    </div>
  );
}
