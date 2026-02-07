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
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={handleClose}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50"
          />

          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
          >
            <div 
              className="bg-dark-900 rounded-2xl shadow-xl border border-dark-800/50 w-full max-w-md overflow-hidden"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="bg-gradient-to-r from-primary-600 to-primary-500 px-6 py-4 flex items-center justify-between">
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

              <div className="p-6">
                {step === 'form' && (
                  <div className="mb-6 p-4 bg-dark-800/50 border border-dark-700/50 rounded-xl">
                    <p className="text-sm text-dark-400 mb-1">Available Balance</p>
                    {isLoadingWallet ? (
                      <Loader2 className="w-5 h-5 text-dark-400 animate-spin" />
                    ) : wallet ? (
                      <p className="text-2xl font-bold text-white">
                        {formatCurrency(wallet.availableBalance)}
                      </p>
                    ) : (
                      <p className="text-dark-500">Unable to load balance</p>
                    )}
                  </div>
                )}

                {step === 'form' && (
                  <form onSubmit={handleSubmit} className="space-y-5">
                    <div>
                      <label className="block text-sm font-medium text-dark-300 mb-2">
                        Send to
                      </label>
                      <UserSearch
                        excludeUserId={userId}
                        onUserSelect={handleUserSelect}
                        selectedUser={selectedUser}
                        onClear={handleClearUser}
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-dark-300 mb-2">
                        Amount
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                          <DollarSign className="h-5 w-5 text-dark-500" />
                        </div>
                        <input
                          type="text"
                          inputMode="decimal"
                          value={amount}
                          onChange={handleAmountChange}
                          placeholder="0.00"
                          className="block w-full pl-10 pr-16 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all text-white text-lg font-medium placeholder-dark-500"
                        />
                        <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                          <span className="text-dark-400 font-medium">USD</span>
                        </div>
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-dark-300 mb-2">
                        Note (optional)
                      </label>
                      <textarea
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="What's this for?"
                        rows={2}
                        maxLength={100}
                        className="block w-full px-4 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all text-white resize-none placeholder-dark-500"
                      />
                    </div>

                    {error && (
                      <div className="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500/30 text-red-400 rounded-xl">
                        <AlertCircle className="w-5 h-5 flex-shrink-0" />
                        <p className="text-sm">{error}</p>
                      </div>
                    )}

                    <button
                      type="submit"
                      disabled={!selectedUser || !amount || isLoadingWallet}
                      className="btn-primary w-full"
                    >
                      Continue
                    </button>
                  </form>
                )}

                {step === 'confirming' && selectedUser && (
                  <div className="space-y-6">
                    <div className="text-center">
                      <p className="text-dark-400 mb-2">You're about to send</p>
                      <p className="text-4xl font-bold text-white mb-2">
                        {formatCurrency(parseFloat(amount))}
                      </p>
                      <p className="text-dark-400">to</p>
                    </div>

                    <div className="flex items-center justify-center gap-3 p-4 bg-dark-800/50 border border-dark-700/50 rounded-xl">
                      {selectedUser.profileImageUrl ? (
                        <img
                          src={selectedUser.profileImageUrl}
                          alt={`${selectedUser.firstName} ${selectedUser.lastName}`}
                          className="w-14 h-14 rounded-full object-cover"
                        />
                      ) : (
                        <div className="w-14 h-14 bg-gradient-to-br from-primary-600 to-primary-500 rounded-full flex items-center justify-center text-white text-lg font-medium shadow-lg shadow-primary-500/25">
                          {getInitials(selectedUser.firstName, selectedUser.lastName)}
                        </div>
                      )}
                      <div>
                        <p className="font-semibold text-white">
                          {selectedUser.firstName} {selectedUser.lastName}
                        </p>
                        <p className="text-sm text-dark-400">{selectedUser.email}</p>
                      </div>
                    </div>

                    {description && (
                      <div className="p-3 bg-primary-500/10 border border-primary-500/30 rounded-xl">
                        <p className="text-sm text-dark-400 mb-1">Note</p>
                        <p className="text-white">{description}</p>
                      </div>
                    )}

                    <div className="flex gap-3">
                      <button
                        onClick={() => setStep('form')}
                        className="btn-secondary flex-1"
                      >
                        Back
                      </button>
                      <button
                        onClick={handleConfirmTransfer}
                        className="btn-primary flex-1"
                      >
                        Confirm & Send
                      </button>
                    </div>
                  </div>
                )}

                {step === 'processing' && (
                  <div className="text-center py-8">
                    <div className="w-16 h-16 bg-primary-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
                      <Loader2 className="w-8 h-8 text-primary-400 animate-spin" />
                    </div>
                    <h3 className="text-lg font-semibold text-white mb-2">
                      Processing Transfer
                    </h3>
                    <p className="text-dark-400">
                      Please wait while we process your transfer...
                    </p>
                  </div>
                )}

                {step === 'success' && transfer && selectedUser && (
                  <div className="text-center py-6">
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ type: 'spring', duration: 0.5 }}
                      className="w-16 h-16 bg-secondary-500/20 rounded-full flex items-center justify-center mx-auto mb-4"
                    >
                      <CheckCircle2 className="w-8 h-8 text-secondary-400" />
                    </motion.div>
                    <h3 className="text-lg font-semibold text-white mb-2">
                      Transfer Successful!
                    </h3>
                    <p className="text-dark-400 mb-6">
                      {formatCurrency(transfer.amount)} has been sent to {selectedUser.firstName}
                    </p>

                    <div className="p-4 bg-dark-800/50 border border-dark-700/50 rounded-xl text-left mb-6">
                      <div className="flex justify-between mb-2">
                        <span className="text-dark-400">Transaction ID</span>
                        <span className="font-mono text-sm text-white">{transfer.transactionReference}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-dark-400">Status</span>
                        <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-secondary-500/20 text-secondary-400">
                          {transfer.status}
                        </span>
                      </div>
                    </div>

                    <button
                      onClick={handleClose}
                      className="btn-primary w-full"
                    >
                      Done
                    </button>
                  </div>
                )}

                {step === 'error' && (
                  <div className="text-center py-6">
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ type: 'spring', duration: 0.5 }}
                      className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-4"
                    >
                      <XCircle className="w-8 h-8 text-red-400" />
                    </motion.div>
                    <h3 className="text-lg font-semibold text-white mb-2">
                      Transfer Failed
                    </h3>
                    <p className="text-dark-400 mb-6">
                      {error || 'Something went wrong. Please try again.'}
                    </p>

                    <div className="flex gap-3">
                      <button
                        onClick={handleClose}
                        className="btn-secondary flex-1"
                      >
                        Close
                      </button>
                      <button
                        onClick={handleTryAgain}
                        className="btn-primary flex-1"
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
