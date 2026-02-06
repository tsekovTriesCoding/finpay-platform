import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  X, 
  ArrowUpRight, 
  Loader2, 
  CheckCircle2, 
  XCircle,
  DollarSign,
  AlertCircle
} from 'lucide-react';

import UserSearch from './UserSearch';
import { paymentService, walletService, UserSearchResult, Wallet, MoneyTransfer } from '../../api';

interface SendMoneyModalProps {
  isOpen: boolean;
  onClose: () => void;
  userId: string;
  onTransferComplete?: (transfer: MoneyTransfer) => void;
}

type TransferStep = 'form' | 'confirming' | 'processing' | 'success' | 'error';

export default function SendMoneyModal({ 
  isOpen, 
  onClose, 
  userId,
  onTransferComplete 
}: SendMoneyModalProps) {
  const [step, setStep] = useState<TransferStep>('form');
  const [selectedUser, setSelectedUser] = useState<UserSearchResult | null>(null);
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transfer, setTransfer] = useState<MoneyTransfer | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoadingWallet, setIsLoadingWallet] = useState(true);

  const loadWallet = useCallback(async () => {
    setIsLoadingWallet(true);
    try {
      const walletData = await walletService.getWallet(userId);
      setWallet(walletData);
    } catch (err) {
      console.error('Error loading wallet:', err);
      setError('Failed to load wallet');
    } finally {
      setIsLoadingWallet(false);
    }
  }, [userId]);

  useEffect(() => {
    if (isOpen && userId) {
      loadWallet();
    }
  }, [isOpen, userId, loadWallet]);

  useEffect(() => {
    if (!isOpen) {
      setTimeout(() => {
        setStep('form');
        setSelectedUser(null);
        setAmount('');
        setDescription('');
        setTransfer(null);
        setError(null);
      }, 300);
    }
  }, [isOpen]);

  const handleUserSelect = (user: UserSearchResult) => {
    setSelectedUser(user);
    setError(null);
  };

  const handleClearUser = () => {
    setSelectedUser(null);
  };

  const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    // Only allow numbers and decimal point
    if (/^\d*\.?\d{0,2}$/.test(value) || value === '') {
      setAmount(value);
      setError(null);
    }
  };

  const validateForm = (): boolean => {
    if (!selectedUser) {
      setError('Please select a recipient');
      return false;
    }

    const amountNum = parseFloat(amount);
    if (!amount || isNaN(amountNum) || amountNum <= 0) {
      setError('Please enter a valid amount');
      return false;
    }

    if (wallet && amountNum > wallet.availableBalance) {
      setError(`Insufficient funds. Available: $${wallet.availableBalance.toFixed(2)}`);
      return false;
    }

    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) return;

    setStep('confirming');
  };

  const handleConfirmTransfer = async () => {
    if (!selectedUser) return;

    setStep('processing');
    setError(null);

    try {
      const transferResult = await paymentService.sendMoney(userId, {
        recipientUserId: selectedUser.id,
        amount: parseFloat(amount),
        currency: 'USD',
        description: description || undefined,
      });

      setTransfer(transferResult);

      if (transferResult.status === 'COMPLETED') {
        setStep('success');
        onTransferComplete?.(transferResult);
      } else if (transferResult.status === 'FAILED' || 
                 transferResult.status === 'COMPENSATED') {
        setError(transferResult.failureReason || 'Transfer failed');
        setStep('error');
      } else {
        setStep('success');
        onTransferComplete?.(transferResult);
      }
    } catch (err) {
      console.error('Transfer error:', err);
      const errorMessage = err instanceof Error ? err.message : 'Transfer failed. Please try again.';
      const apiError = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(apiError || errorMessage);
      setStep('error');
    }
  };

  const handleClose = () => {
    if (step === 'processing') return; // Don't allow closing during processing
    onClose();
  };

  const handleTryAgain = () => {
    setStep('form');
    setError(null);
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value);
  };

  const getInitials = (firstName: string, lastName: string) => {
    return `${firstName?.[0] || ''}${lastName?.[0] || ''}`.toUpperCase();
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={handleClose}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50"
          />

          {/* Modal */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
          >
            <div 
              className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className="bg-gradient-to-r from-blue-600 to-purple-600 px-6 py-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                    <ArrowUpRight className="w-5 h-5 text-white" />
                  </div>
                  <h2 className="text-xl font-semibold text-white">Send Money</h2>
                </div>
                {step !== 'processing' && (
                  <button
                    onClick={handleClose}
                    className="p-2 text-white/80 hover:text-white hover:bg-white/10 rounded-full transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                )}
              </div>

              {/* Content */}
              <div className="p-6">
                {/* Wallet Balance Display */}
                {step === 'form' && (
                  <div className="mb-6 p-4 bg-gray-50 rounded-xl">
                    <p className="text-sm text-gray-500 mb-1">Available Balance</p>
                    {isLoadingWallet ? (
                      <Loader2 className="w-5 h-5 text-gray-400 animate-spin" />
                    ) : wallet ? (
                      <p className="text-2xl font-bold text-gray-900">
                        {formatCurrency(wallet.availableBalance)}
                      </p>
                    ) : (
                      <p className="text-gray-400">Unable to load balance</p>
                    )}
                  </div>
                )}

                {/* Form Step */}
                {step === 'form' && (
                  <form onSubmit={handleSubmit} className="space-y-5">
                    {/* Recipient Search */}
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Send to
                      </label>
                      <UserSearch
                        excludeUserId={userId}
                        onUserSelect={handleUserSelect}
                        selectedUser={selectedUser}
                        onClear={handleClearUser}
                      />
                    </div>

                    {/* Amount Input */}
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Amount
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                          <DollarSign className="h-5 w-5 text-gray-400" />
                        </div>
                        <input
                          type="text"
                          inputMode="decimal"
                          value={amount}
                          onChange={handleAmountChange}
                          placeholder="0.00"
                          className="block w-full pl-10 pr-16 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900 text-lg font-medium"
                        />
                        <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                          <span className="text-gray-400 font-medium">USD</span>
                        </div>
                      </div>
                    </div>

                    {/* Description Input */}
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Note (optional)
                      </label>
                      <textarea
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="What's this for?"
                        rows={2}
                        maxLength={100}
                        className="block w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900 resize-none"
                      />
                    </div>

                    {/* Error Display */}
                    {error && (
                      <div className="flex items-center gap-2 p-3 bg-red-50 text-red-700 rounded-xl">
                        <AlertCircle className="w-5 h-5 flex-shrink-0" />
                        <p className="text-sm">{error}</p>
                      </div>
                    )}

                    {/* Submit Button */}
                    <button
                      type="submit"
                      disabled={!selectedUser || !amount || isLoadingWallet}
                      className="w-full py-3 px-4 bg-gradient-to-r from-blue-600 to-purple-600 text-white font-medium rounded-xl hover:from-blue-700 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                    >
                      Continue
                    </button>
                  </form>
                )}

                {/* Confirmation Step */}
                {step === 'confirming' && selectedUser && (
                  <div className="space-y-6">
                    <div className="text-center">
                      <p className="text-gray-500 mb-2">You're about to send</p>
                      <p className="text-4xl font-bold text-gray-900 mb-2">
                        {formatCurrency(parseFloat(amount))}
                      </p>
                      <p className="text-gray-500">to</p>
                    </div>

                    <div className="flex items-center justify-center gap-3 p-4 bg-gray-50 rounded-xl">
                      {selectedUser.profileImageUrl ? (
                        <img
                          src={selectedUser.profileImageUrl}
                          alt={`${selectedUser.firstName} ${selectedUser.lastName}`}
                          className="w-14 h-14 rounded-full object-cover"
                        />
                      ) : (
                        <div className="w-14 h-14 bg-gradient-to-br from-blue-600 to-purple-600 rounded-full flex items-center justify-center text-white text-lg font-medium">
                          {getInitials(selectedUser.firstName, selectedUser.lastName)}
                        </div>
                      )}
                      <div>
                        <p className="font-semibold text-gray-900">
                          {selectedUser.firstName} {selectedUser.lastName}
                        </p>
                        <p className="text-sm text-gray-500">{selectedUser.email}</p>
                      </div>
                    </div>

                    {description && (
                      <div className="p-3 bg-blue-50 rounded-xl">
                        <p className="text-sm text-gray-500 mb-1">Note</p>
                        <p className="text-gray-900">{description}</p>
                      </div>
                    )}

                    <div className="flex gap-3">
                      <button
                        onClick={() => setStep('form')}
                        className="flex-1 py-3 px-4 border border-gray-300 text-gray-700 font-medium rounded-xl hover:bg-gray-50 transition-colors"
                      >
                        Back
                      </button>
                      <button
                        onClick={handleConfirmTransfer}
                        className="flex-1 py-3 px-4 bg-gradient-to-r from-blue-600 to-purple-600 text-white font-medium rounded-xl hover:from-blue-700 hover:to-purple-700 transition-all"
                      >
                        Confirm & Send
                      </button>
                    </div>
                  </div>
                )}

                {/* Processing Step */}
                {step === 'processing' && (
                  <div className="text-center py-8">
                    <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                      <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      Processing Transfer
                    </h3>
                    <p className="text-gray-500">
                      Please wait while we process your transfer...
                    </p>
                  </div>
                )}

                {/* Success Step */}
                {step === 'success' && transfer && selectedUser && (
                  <div className="text-center py-6">
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ type: 'spring', duration: 0.5 }}
                      className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4"
                    >
                      <CheckCircle2 className="w-8 h-8 text-green-600" />
                    </motion.div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      Transfer Successful!
                    </h3>
                    <p className="text-gray-500 mb-6">
                      {formatCurrency(transfer.amount)} has been sent to {selectedUser.firstName}
                    </p>

                    <div className="p-4 bg-gray-50 rounded-xl text-left mb-6">
                      <div className="flex justify-between mb-2">
                        <span className="text-gray-500">Transaction ID</span>
                        <span className="font-mono text-sm text-gray-900">{transfer.transactionReference}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-500">Status</span>
                        <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                          {transfer.status}
                        </span>
                      </div>
                    </div>

                    <button
                      onClick={handleClose}
                      className="w-full py-3 px-4 bg-gradient-to-r from-blue-600 to-purple-600 text-white font-medium rounded-xl hover:from-blue-700 hover:to-purple-700 transition-all"
                    >
                      Done
                    </button>
                  </div>
                )}

                {/* Error Step */}
                {step === 'error' && (
                  <div className="text-center py-6">
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ type: 'spring', duration: 0.5 }}
                      className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4"
                    >
                      <XCircle className="w-8 h-8 text-red-600" />
                    </motion.div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      Transfer Failed
                    </h3>
                    <p className="text-gray-500 mb-6">
                      {error || 'Something went wrong. Please try again.'}
                    </p>

                    <div className="flex gap-3">
                      <button
                        onClick={handleClose}
                        className="flex-1 py-3 px-4 border border-gray-300 text-gray-700 font-medium rounded-xl hover:bg-gray-50 transition-colors"
                      >
                        Close
                      </button>
                      <button
                        onClick={handleTryAgain}
                        className="flex-1 py-3 px-4 bg-gradient-to-r from-blue-600 to-purple-600 text-white font-medium rounded-xl hover:from-blue-700 hover:to-purple-700 transition-all"
                      >
                        Try Again
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
