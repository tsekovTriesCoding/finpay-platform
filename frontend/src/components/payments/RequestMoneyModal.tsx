import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X,
  ArrowDownLeft,
  Loader2,
  CheckCircle2,
  XCircle,
  DollarSign,
  AlertCircle,
  Clock,
} from 'lucide-react';

import UserSearch from './UserSearch';
import { UserSearchResult, MoneyRequest } from '../../api';
import { useCreateMoneyRequest } from '../../hooks';

interface RequestMoneyModalProps {
  isOpen: boolean;
  onClose: () => void;
  userId: string;
  onRequestCreated?: (request: MoneyRequest) => void;
}

type RequestStep = 'form' | 'confirming' | 'processing' | 'success' | 'error';

export default function RequestMoneyModal({
  isOpen,
  onClose,
  userId,
  onRequestCreated,
}: RequestMoneyModalProps) {
  const [step, setStep] = useState<RequestStep>('form');
  const [selectedUser, setSelectedUser] = useState<UserSearchResult | null>(null);
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [request, setRequest] = useState<MoneyRequest | null>(null);
  const [error, setError] = useState<string | null>(null);

  const createRequest = useCreateMoneyRequest(userId);

  useEffect(() => {
    if (!isOpen) {
      setTimeout(() => {
        setStep('form');
        setSelectedUser(null);
        setAmount('');
        setDescription('');
        setRequest(null);
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
    if (/^\d*\.?\d{0,2}$/.test(value) || value === '') {
      setAmount(value);
      setError(null);
    }
  };

  const validateForm = (): boolean => {
    if (!selectedUser) {
      setError('Please select who to request money from');
      return false;
    }
    const amountNum = parseFloat(amount);
    if (!amount || isNaN(amountNum) || amountNum <= 0) {
      setError('Please enter a valid amount');
      return false;
    }
    return true;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;
    setStep('confirming');
  };

  const handleConfirmRequest = () => {
    if (!selectedUser) return;

    setStep('processing');
    setError(null);

    createRequest.mutate(
      {
        payerUserId: selectedUser.id,
        amount: parseFloat(amount),
        currency: 'USD',
        description: description || undefined,
      },
      {
        onSuccess: (result) => {
          setRequest(result);
          setStep('success');
          onRequestCreated?.(result);
        },
        onError: (err) => {
          console.error('Request money error:', err);
          const apiError = (err as { response?: { data?: { message?: string } } })?.response
            ?.data?.message;
          setError(apiError || err.message || 'Failed to create request. Please try again.');
          setStep('error');
        },
      },
    );
  };

  const handleClose = () => {
    if (step === 'processing') return;
    onClose();
  };

  const handleTryAgain = () => {
    setStep('form');
    setError(null);
  };

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);

  const getInitials = (firstName: string, lastName: string) =>
    `${firstName?.[0] || ''}${lastName?.[0] || ''}`.toUpperCase();

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
              {/* Header */}
              <div className="bg-gradient-to-r from-green-600 to-green-500 px-6 py-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                    <ArrowDownLeft className="w-5 h-5 text-white" />
                  </div>
                  <h2 className="text-xl font-semibold text-white">Request Money</h2>
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

              {/* Body */}
              <div className="p-6">
                {/* ── Form step ─────────────────────────────── */}
                {step === 'form' && (
                  <form onSubmit={handleSubmit} className="space-y-5">
                    <div>
                      <label className="block text-sm font-medium text-dark-300 mb-2">
                        Request from
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
                          className="block w-full pl-10 pr-16 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all text-white text-lg font-medium placeholder-dark-500"
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
                        className="block w-full px-4 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all text-white resize-none placeholder-dark-500"
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
                      disabled={!selectedUser || !amount}
                      className="w-full py-3 px-4 bg-gradient-to-r from-green-600 to-green-500 text-white font-semibold rounded-xl hover:from-green-700 hover:to-green-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-lg shadow-green-500/25"
                    >
                      Continue
                    </button>
                  </form>
                )}

                {/* ── Confirming step ──────────────────────── */}
                {step === 'confirming' && selectedUser && (
                  <div className="space-y-6">
                    <div className="text-center">
                      <p className="text-dark-400 mb-2">You're requesting</p>
                      <p className="text-4xl font-bold text-white mb-2">
                        {formatCurrency(parseFloat(amount))}
                      </p>
                      <p className="text-dark-400">from</p>
                    </div>

                    <div className="flex items-center justify-center gap-3 p-4 bg-dark-800/50 border border-dark-700/50 rounded-xl">
                      {selectedUser.profileImageUrl ? (
                        <img
                          src={selectedUser.profileImageUrl}
                          alt={`${selectedUser.firstName} ${selectedUser.lastName}`}
                          className="w-14 h-14 rounded-full object-cover"
                        />
                      ) : (
                        <div className="w-14 h-14 bg-gradient-to-br from-green-600 to-green-500 rounded-full flex items-center justify-center text-white text-lg font-medium shadow-lg shadow-green-500/25">
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
                      <div className="p-3 bg-green-500/10 border border-green-500/30 rounded-xl">
                        <p className="text-sm text-dark-400 mb-1">Note</p>
                        <p className="text-white">{description}</p>
                      </div>
                    )}

                    <div className="p-3 bg-dark-800/50 border border-dark-700/50 rounded-xl flex items-center gap-2">
                      <Clock className="w-4 h-4 text-dark-400 flex-shrink-0" />
                      <p className="text-sm text-dark-400">
                        This request will expire in 7 days if not acted upon.
                      </p>
                    </div>

                    <div className="flex gap-3">
                      <button onClick={() => setStep('form')} className="btn-secondary flex-1">
                        Back
                      </button>
                      <button
                        onClick={handleConfirmRequest}
                        className="flex-1 py-3 px-4 bg-gradient-to-r from-green-600 to-green-500 text-white font-semibold rounded-xl hover:from-green-700 hover:to-green-600 transition-all shadow-lg shadow-green-500/25"
                      >
                        Send Request
                      </button>
                    </div>
                  </div>
                )}

                {/* ── Processing step ──────────────────────── */}
                {step === 'processing' && (
                  <div className="text-center py-8">
                    <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
                      <Loader2 className="w-8 h-8 text-green-400 animate-spin" />
                    </div>
                    <h3 className="text-lg font-semibold text-white mb-2">Sending Request</h3>
                    <p className="text-dark-400">Please wait...</p>
                  </div>
                )}

                {/* ── Success step ─────────────────────────── */}
                {step === 'success' && request && selectedUser && (
                  <div className="text-center py-6">
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ type: 'spring', duration: 0.5 }}
                      className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-4"
                    >
                      <CheckCircle2 className="w-8 h-8 text-green-400" />
                    </motion.div>
                    <h3 className="text-lg font-semibold text-white mb-2">Request Sent!</h3>
                    <p className="text-dark-400 mb-6">
                      {formatCurrency(request.amount)} requested from {selectedUser.firstName}
                    </p>

                    <div className="p-4 bg-dark-800/50 border border-dark-700/50 rounded-xl text-left mb-6">
                      <div className="flex justify-between mb-2">
                        <span className="text-dark-400">Request ID</span>
                        <span className="font-mono text-sm text-white">
                          {request.requestReference}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-dark-400">Status</span>
                        <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-500/20 text-yellow-400">
                          Awaiting Approval
                        </span>
                      </div>
                    </div>

                    <button onClick={handleClose} className="btn-primary w-full">
                      Done
                    </button>
                  </div>
                )}

                {/* ── Error step ───────────────────────────── */}
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
                    <h3 className="text-lg font-semibold text-white mb-2">Request Failed</h3>
                    <p className="text-dark-400 mb-6">
                      {error || 'Something went wrong. Please try again.'}
                    </p>

                    <div className="flex gap-3">
                      <button onClick={handleClose} className="btn-secondary flex-1">
                        Close
                      </button>
                      <button onClick={handleTryAgain} className="btn-primary flex-1">
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
