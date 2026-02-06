import { useState, useEffect, useCallback } from 'react';
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
import { paymentService, walletService, Wallet as WalletType, MoneyTransfer } from '../api';
import { SendMoneyModal } from '../components/payments';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [wallet, setWallet] = useState<WalletType | null>(null);
  const [isLoadingWallet, setIsLoadingWallet] = useState(true);
  const [isSendMoneyOpen, setIsSendMoneyOpen] = useState(false);
  const [recentTransfers, setRecentTransfers] = useState<MoneyTransfer[]>([]);

  const loadWallet = useCallback(async () => {
    if (!user?.id) return;
    setIsLoadingWallet(true);
    try {
      const walletData = await walletService.getWallet(user.id);
      setWallet(walletData);
    } catch (err) {
      console.error('Error loading wallet:', err);
    } finally {
      setIsLoadingWallet(false);
    }
  }, [user?.id]);

  const loadRecentTransfers = useCallback(async () => {
    if (!user?.id) return;
    try {
      const response = await paymentService.getTransferHistory(user.id, 0, 5);
      setRecentTransfers(response.content);
    } catch (err) {
      console.error('Error loading transfers:', err);
    }
  }, [user?.id]);

  useEffect(() => {
    if (user?.id) {
      loadWallet();
      loadRecentTransfers();
    }
  }, [user?.id, loadWallet, loadRecentTransfers]);

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const handleSendMoney = () => {
    setIsSendMoneyOpen(true);
  };

  const handleTransferComplete = (_transfer: MoneyTransfer) => {
    loadWallet();
    loadRecentTransfers();
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
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-600 to-purple-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold">F</span>
              </div>
              <span className="text-xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
                FinPay
              </span>
            </div>

            <div className="flex items-center gap-4">
              <button className="p-2 text-gray-400 hover:text-gray-600 transition-colors">
                <Settings className="w-5 h-5" />
              </button>
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-purple-600 rounded-full flex items-center justify-center text-white font-medium">
                  {user?.firstName?.[0]}{user?.lastName?.[0]}
                </div>
                <div className="hidden sm:block">
                  <p className="text-sm font-medium text-gray-900">
                    {user?.firstName} {user?.lastName}
                  </p>
                  <p className="text-xs text-gray-500">{user?.email}</p>
                </div>
              </div>
              <button
                onClick={handleLogout}
                className="p-2 text-gray-400 hover:text-red-600 transition-colors"
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
          <h1 className="text-2xl font-bold text-gray-900">
            Welcome back, {user?.firstName}!
          </h1>
          <p className="text-gray-500">Here's your financial overview</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="bg-gradient-to-br from-blue-600 to-purple-600 rounded-2xl p-6 text-white mb-8"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <Wallet className="w-6 h-6" />
              <span className="text-blue-100">Available Balance</span>
            </div>
            <button
              onClick={loadWallet}
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
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {quickActions.map((action, index) => (
              <button
                key={index}
                onClick={action.onClick}
                className="bg-white rounded-xl p-4 shadow-sm hover:shadow-md transition-all hover:scale-[1.02] text-center"
              >
                <div className={`w-12 h-12 ${action.color} rounded-full flex items-center justify-center mx-auto mb-3`}>
                  <action.icon className="w-6 h-6 text-white" />
                </div>
                <p className="text-sm font-medium text-gray-900">{action.label}</p>
              </button>
            ))}
          </div>
        </motion.div>

        {/* Recent Transfers */}
        {recentTransfers.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.25 }}
            className="bg-white rounded-xl p-6 shadow-sm mb-8"
          >
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Recent Transfers</h2>
            <div className="space-y-3">
              {recentTransfers.map((transfer) => (
                <div 
                  key={transfer.id}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                      transfer.senderUserId === user?.id 
                        ? 'bg-red-100' 
                        : 'bg-green-100'
                    }`}>
                      {transfer.senderUserId === user?.id ? (
                        <ArrowUpRight className="w-5 h-5 text-red-600" />
                      ) : (
                        <ArrowDownLeft className="w-5 h-5 text-green-600" />
                      )}
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">
                        {transfer.senderUserId === user?.id ? 'Sent' : 'Received'}
                      </p>
                      <p className="text-sm text-gray-500">
                        {transfer.description || transfer.transactionReference}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className={`font-semibold ${
                      transfer.senderUserId === user?.id 
                        ? 'text-red-600' 
                        : 'text-green-600'
                    }`}>
                      {transfer.senderUserId === user?.id ? '-' : '+'}
                      {formatCurrency(transfer.amount)}
                    </p>
                    <p className={`text-xs font-medium px-2 py-0.5 rounded-full inline-block ${
                      transfer.status === 'COMPLETED' 
                        ? 'bg-green-100 text-green-700'
                        : transfer.status === 'FAILED' || transfer.status === 'COMPENSATED'
                        ? 'bg-red-100 text-red-700'
                        : 'bg-yellow-100 text-yellow-700'
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
          className="bg-white rounded-xl p-6 shadow-sm"
        >
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Account Status</h2>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <User className="w-5 h-5 text-gray-400" />
                <span className="text-gray-600">Account Status</span>
              </div>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                user?.status === 'ACTIVE' 
                  ? 'bg-green-100 text-green-700'
                  : user?.status === 'PENDING_VERIFICATION'
                  ? 'bg-yellow-100 text-yellow-700'
                  : 'bg-gray-100 text-gray-700'
              }`}>
                {user?.status?.replace('_', ' ')}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Email Verified</span>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                user?.emailVerified 
                  ? 'bg-green-100 text-green-700'
                  : 'bg-red-100 text-red-700'
              }`}>
                {user?.emailVerified ? 'Verified' : 'Not Verified'}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Role</span>
              <span className="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm font-medium">
                {user?.role}
              </span>
            </div>
          </div>
        </motion.div>

        <div className="mt-8 text-center">
          <Link 
            to="/"
            className="text-blue-600 hover:text-blue-500 text-sm"
          >
            ← Back to Home
          </Link>
        </div>
      </main>

      {/* Send Money Modal */}
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
