import { useState } from 'react';
import { Zap, Sparkles, Building2, Check, ArrowUpRight, Crown } from 'lucide-react';
import { motion } from 'framer-motion';

import type { AccountPlan } from '../../api/authApi';
import UpgradePlanModal from './UpgradePlanModal';

const PLANS: {
  key: AccountPlan;
  label: string;
  icon: typeof Zap;
  price: string;
  period: string;
  description: string;
  gradient: string;
  border: string;
  badge: string;
  features: string[];
}[] = [
  {
    key: 'STARTER',
    label: 'Starter',
    icon: Zap,
    price: '$0',
    period: 'Free forever',
    description: 'Great for getting started with digital payments.',
    gradient: 'from-dark-600 to-dark-700',
    border: 'border-dark-600',
    badge: 'bg-dark-700/60 text-dark-300',
    features: [
      '10 transactions/month',
      '1 virtual card',
      'Basic analytics dashboard',
      'Email support',
    ],
  },
  {
    key: 'PRO',
    label: 'Pro',
    icon: Sparkles,
    price: '$29',
    period: '/month',
    description: 'For growing businesses that need more power.',
    gradient: 'from-primary-600 to-primary-500',
    border: 'border-primary-500/30',
    badge: 'bg-primary-500/15 text-primary-300',
    features: [
      'Unlimited transactions',
      '10 virtual cards',
      'Multi-currency accounts',
      'Priority support',
      'API access',
    ],
  },
  {
    key: 'ENTERPRISE',
    label: 'Enterprise',
    icon: Building2,
    price: 'Custom',
    period: 'pricing',
    description: 'Full-featured solution for large organizations.',
    gradient: 'from-secondary-600 to-secondary-500',
    border: 'border-secondary-500/30',
    badge: 'bg-secondary-500/15 text-secondary-300',
    features: [
      'Everything in Pro',
      'Unlimited virtual cards',
      'Dedicated account manager',
      'Custom integrations',
      'SLA guarantee (99.99%)',
    ],
  },
];

const PLAN_ORDER: AccountPlan[] = ['STARTER', 'PRO', 'ENTERPRISE'];

interface PlanBillingTabProps {
  currentPlan: AccountPlan;
}

export default function PlanBillingTab({ currentPlan }: PlanBillingTabProps) {
  const [upgradeTarget, setUpgradeTarget] = useState<AccountPlan | null>(null);

  const currentIndex = PLAN_ORDER.indexOf(currentPlan);

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      {(() => {
        const plan = PLANS.find((p) => p.key === currentPlan)!;
        const Icon = plan.icon;
        return (
          <div className="bg-dark-800/50 border border-dark-700/50 rounded-2xl p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div
                  className={`w-12 h-12 rounded-xl bg-gradient-to-br ${plan.gradient} flex items-center justify-center shadow-lg`}
                >
                  <Icon className="w-6 h-6 text-white" />
                </div>
                <div>
                  <p className="text-sm text-dark-400">Current Plan</p>
                  <p className="text-xl font-bold text-white">{plan.label}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-2xl font-bold text-white">{plan.price}</p>
                <p className="text-sm text-dark-400">{plan.period}</p>
              </div>
            </div>
            {currentPlan === 'ENTERPRISE' && (
              <div className="mt-4 flex items-center gap-2 text-sm text-secondary-400">
                <Crown className="w-4 h-4" />
                You're on the highest tier - enjoy all features!
              </div>
            )}
          </div>
        );
      })()}

      <div className="bg-dark-800/50 border border-dark-700/50 rounded-2xl p-6">
        <h3 className="text-lg font-semibold text-white mb-2">
          {currentPlan === 'ENTERPRISE' ? 'Plan Overview' : 'Available Plans'}
        </h3>
        <p className="text-sm text-dark-400 mb-6">
          {currentPlan === 'ENTERPRISE'
            ? 'Your current plan includes all features.'
            : 'Compare plans and upgrade instantly. Changes take effect immediately.'}
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {PLANS.map((plan) => {
            const Icon = plan.icon;
            const planIndex = PLAN_ORDER.indexOf(plan.key);
            const isCurrent = plan.key === currentPlan;
            const isUpgrade = planIndex > currentIndex;
            const isDowngrade = planIndex < currentIndex;

            return (
              <div
                key={plan.key}
                className={`relative flex flex-col rounded-xl border p-5 transition-all ${
                  isCurrent
                    ? `${plan.border} ring-2 ring-primary-500/20 bg-gradient-to-br ${plan.gradient} bg-opacity-5`
                    : isUpgrade
                      ? 'border-dark-600 hover:border-dark-500 bg-dark-900/30'
                      : 'border-dark-700/50 bg-dark-900/20 opacity-60'
                }`}
              >
                {isCurrent && (
                  <span className="absolute -top-2.5 left-4 px-2.5 py-0.5 bg-primary-500 text-white text-[10px] font-bold rounded-full uppercase tracking-wider">
                    Current
                  </span>
                )}

                <div className="flex items-center gap-3 mb-3 mt-1">
                  <div
                    className={`w-9 h-9 rounded-lg bg-gradient-to-br ${plan.gradient} flex items-center justify-center`}
                  >
                    <Icon className="w-4.5 h-4.5 text-white" />
                  </div>
                  <div>
                    <p className="text-white font-semibold">{plan.label}</p>
                    <p className="text-xs text-dark-400">
                      {plan.price === 'Custom' ? 'Custom pricing' : `${plan.price}${plan.period}`}
                    </p>
                  </div>
                </div>

                <p className="text-xs text-dark-400 mb-4">{plan.description}</p>

                <ul className="space-y-2 mb-5 flex-1">
                  {plan.features.map((feature) => (
                    <li
                      key={feature}
                      className="flex items-start gap-2 text-xs text-dark-300"
                    >
                      <Check className="w-3.5 h-3.5 text-green-400 shrink-0 mt-0.5" />
                      {feature}
                    </li>
                  ))}
                </ul>

                {isCurrent ? (
                  <div className="text-center text-xs text-dark-400 font-medium py-2">
                    Active
                  </div>
                ) : isUpgrade ? (
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setUpgradeTarget(plan.key)}
                    className="w-full flex items-center justify-center gap-1.5 py-2.5 bg-gradient-to-r from-primary-600 to-primary-500 hover:from-primary-500 hover:to-primary-400 rounded-xl text-sm font-medium text-white transition-all shadow-lg shadow-primary-500/25"
                  >
                    Upgrade
                    <ArrowUpRight className="w-3.5 h-3.5" />
                  </motion.button>
                ) : (
                  <div className="text-center text-xs text-dark-500 py-2">
                    {isDowngrade ? 'Included in your plan' : ''}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {upgradeTarget && (
        <UpgradePlanModal
          isOpen={!!upgradeTarget}
          onClose={() => setUpgradeTarget(null)}
          currentPlan={currentPlan}
          targetPlan={upgradeTarget}
        />
      )}
    </motion.div>
  );
}
