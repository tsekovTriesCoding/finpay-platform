import { motion, AnimatePresence } from 'framer-motion';
import {
  X,
  Loader2,
  CheckCircle2,
  XCircle,
  ArrowRight,
  Zap,
  Sparkles,
  Building2,
  Check,
} from 'lucide-react';

import type { AccountPlan } from '../../api/authApi';
import { getApiErrorMessage } from '../../api/axios';
import { useUpgradePlan } from '../../hooks/useUpgradePlan';

const PLAN_CONFIG: Record<
  AccountPlan,
  {
    label: string;
    icon: typeof Zap;
    price: string;
    gradient: string;
    features: string[];
  }
> = {
  STARTER: {
    label: 'Starter',
    icon: Zap,
    price: 'Free',
    gradient: 'from-dark-600 to-dark-700',
    features: [
      '10 transactions/month',
      '1 virtual card',
      'Basic analytics',
      'Email support',
    ],
  },
  PRO: {
    label: 'Pro',
    icon: Sparkles,
    price: '$29/mo',
    gradient: 'from-primary-600 to-primary-500',
    features: [
      'Unlimited transactions',
      '10 virtual cards',
      'Multi-currency accounts',
      'Priority support',
      'API access',
    ],
  },
  ENTERPRISE: {
    label: 'Enterprise',
    icon: Building2,
    price: 'Custom',
    gradient: 'from-secondary-600 to-secondary-500',
    features: [
      'Everything in Pro',
      'Unlimited cards',
      'Dedicated account manager',
      'Custom integrations',
      'SLA guarantee (99.99%)',
    ],
  },
};

interface UpgradePlanModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentPlan: AccountPlan;
  targetPlan: AccountPlan;
}

export default function UpgradePlanModal({
  isOpen,
  onClose,
  currentPlan,
  targetPlan,
}: UpgradePlanModalProps) {
  const { mutateAsync: upgradePlan, isPending, isSuccess, isError, error, reset } = useUpgradePlan();

  const step = isPending ? 'processing' : isSuccess ? 'success' : isError ? 'error' : 'confirm';
  const errorMessage = getApiErrorMessage(error, 'Failed to upgrade plan. Please try again.');

  const current = PLAN_CONFIG[currentPlan];
  const target = PLAN_CONFIG[targetPlan];
  const TargetIcon = target.icon;

  const handleUpgrade = async () => {
    try {
      await upgradePlan(targetPlan);
    } catch {
      // Error captured by mutation state
    }
  };

  const handleClose = () => {
    onClose();
    setTimeout(() => reset(), 300);
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={step !== 'processing' ? handleClose : undefined}
          />

          <motion.div
            className="relative w-full max-w-md bg-dark-800 border border-dark-700 rounded-2xl shadow-2xl overflow-hidden"
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            transition={{ duration: 0.2 }}
          >
            <div className="flex items-center justify-between p-6 border-b border-dark-700">
              <h2 className="text-lg font-semibold text-white">Upgrade Plan</h2>
              {step !== 'processing' && (
                <button
                  onClick={handleClose}
                  className="p-1.5 rounded-lg hover:bg-dark-700 text-dark-400 hover:text-white transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              )}
            </div>

            <div className="p-6">
              {step === 'confirm' && (
                <div className="space-y-6">
                  <div className="flex items-center gap-4">
                    <div className="flex-1 rounded-xl border border-dark-600 p-4 text-center">
                      <p className="text-xs text-dark-400 mb-1">Current</p>
                      <p className="text-white font-semibold">{current.label}</p>
                      <p className="text-dark-400 text-sm">{current.price}</p>
                    </div>
                    <ArrowRight className="w-5 h-5 text-primary-400 shrink-0" />
                    <div className={`flex-1 rounded-xl border border-primary-500/30 p-4 text-center bg-gradient-to-br ${target.gradient} bg-opacity-10`}>
                      <p className="text-xs text-primary-300 mb-1">New Plan</p>
                      <p className="text-white font-semibold">{target.label}</p>
                      <p className="text-primary-300 text-sm">{target.price}</p>
                    </div>
                  </div>

                  <div>
                    <p className="text-sm font-medium text-dark-300 mb-3">
                      What you'll get with {target.label}:
                    </p>
                    <ul className="space-y-2">
                      {target.features.map((feature) => (
                        <li key={feature} className="flex items-center gap-2.5 text-sm text-dark-300">
                          <Check className="w-4 h-4 text-green-400 shrink-0" />
                          {feature}
                        </li>
                      ))}
                    </ul>
                  </div>

                  <div className="flex gap-3 pt-2">
                    <button
                      onClick={handleClose}
                      className="flex-1 px-4 py-2.5 bg-dark-700 hover:bg-dark-600 text-white rounded-xl font-medium transition-colors"
                    >
                      Cancel
                    </button>
                    <motion.button
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      onClick={handleUpgrade}
                      className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 bg-gradient-to-r from-primary-600 to-primary-500 hover:from-primary-500 hover:to-primary-400 text-white rounded-xl font-medium transition-all shadow-lg shadow-primary-500/25"
                    >
                      <TargetIcon className="w-4 h-4" />
                      Upgrade Now
                    </motion.button>
                  </div>
                </div>
              )}

              {step === 'processing' && (
                <div className="flex flex-col items-center py-8 gap-4">
                  <Loader2 className="w-10 h-10 text-primary-400 animate-spin" />
                  <p className="text-white font-medium">Upgrading your plan...</p>
                  <p className="text-dark-400 text-sm text-center">
                    Please wait while we update your account to {target.label}.
                  </p>
                </div>
              )}

              {step === 'success' && (
                <div className="flex flex-col items-center py-8 gap-4">
                  <div className="w-16 h-16 rounded-full bg-green-500/10 flex items-center justify-center">
                    <CheckCircle2 className="w-8 h-8 text-green-400" />
                  </div>
                  <p className="text-white font-semibold text-lg">Upgrade Successful!</p>
                  <p className="text-dark-400 text-sm text-center">
                    Your account has been upgraded to {target.label}. Your new limits and features
                    are now active.
                  </p>
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={handleClose}
                    className="mt-2 px-6 py-2.5 bg-primary-600 hover:bg-primary-500 text-white rounded-xl font-medium transition-colors"
                  >
                    Done
                  </motion.button>
                </div>
              )}

              {step === 'error' && (
                <div className="flex flex-col items-center py-8 gap-4">
                  <div className="w-16 h-16 rounded-full bg-red-500/10 flex items-center justify-center">
                    <XCircle className="w-8 h-8 text-red-400" />
                  </div>
                  <p className="text-white font-semibold text-lg">Upgrade Failed</p>
                  <p className="text-red-400 text-sm text-center">{errorMessage}</p>
                  <div className="flex gap-3 mt-2">
                    <button
                      onClick={handleClose}
                      className="px-5 py-2.5 bg-dark-700 hover:bg-dark-600 text-white rounded-xl font-medium transition-colors"
                    >
                      Close
                    </button>
                    <motion.button
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      onClick={() => reset()}
                      className="px-5 py-2.5 bg-primary-600 hover:bg-primary-500 text-white rounded-xl font-medium transition-colors"
                    >
                      Try Again
                    </motion.button>
                  </div>
                </div>
              )}
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
