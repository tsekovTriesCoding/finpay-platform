import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Check, ArrowRight } from 'lucide-react';

import type { AccountPlan } from '../../api';
import { PLANS } from './constants';

interface PlanSelectionStepProps {
  selectedPlan: AccountPlan | null;
  onSelect: (plan: AccountPlan) => void;
  onContinue: () => void;
}

export default function PlanSelectionStep({
  selectedPlan,
  onSelect,
  onContinue,
}: PlanSelectionStepProps) {
  return (
    <motion.div
      key="step-plan"
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      transition={{ duration: 0.3 }}
    >
      <div className="text-center mb-6">
        <h2 className="text-xl font-bold text-white">Choose your plan</h2>
        <p className="text-dark-400 mt-1 text-sm">
          Select the plan that fits your needs. You can upgrade anytime.
        </p>
      </div>

      <div className="space-y-3">
        {PLANS.map((plan) => {
          const isSelected = selectedPlan === plan.id;
          return (
            <motion.button
              key={plan.id}
              type="button"
              onClick={() => onSelect(plan.id)}
              className={`w-full text-left p-4 rounded-xl border-2 transition-all ${
                isSelected
                  ? 'border-primary-500 bg-primary-500/5 shadow-lg shadow-primary-500/10'
                  : 'border-dark-700/50 bg-dark-800/30 hover:border-dark-600 hover:bg-dark-800/50'
              }`}
              whileHover={{ scale: 1.01 }}
              whileTap={{ scale: 0.99 }}
            >
              <div className="flex items-start gap-4">
                <div
                  className={`w-10 h-10 rounded-lg bg-gradient-to-br ${plan.gradient} flex items-center justify-center shrink-0`}
                >
                  <plan.icon className="w-5 h-5 text-white" />
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <h3 className="text-base font-semibold text-white">{plan.name}</h3>
                      {plan.popular && (
                        <span className="px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider rounded-full bg-primary-500/20 text-primary-400">
                          Popular
                        </span>
                      )}
                    </div>
                    <div className="text-right">
                      {plan.price === 'Custom' ? (
                        <span className="text-lg font-bold text-white">Custom</span>
                      ) : (
                        <div>
                          <span className="text-lg font-bold text-white">${plan.price}</span>
                          <span className="text-dark-400 text-xs ml-0.5">{plan.period}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <p className="text-dark-400 text-xs mt-1">{plan.description}</p>

                  <div className="flex flex-wrap gap-x-4 gap-y-1 mt-2">
                    <span className="text-xs text-dark-500">
                      Daily limit: <span className="text-dark-300">{plan.limits.daily}</span>
                    </span>
                    <span className="text-xs text-dark-500">
                      Monthly limit: <span className="text-dark-300">{plan.limits.monthly}</span>
                    </span>
                  </div>

                  {isSelected && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      transition={{ duration: 0.2 }}
                      className="overflow-hidden"
                    >
                      <ul className="mt-3 grid grid-cols-2 gap-x-2 gap-y-1">
                        {plan.features.map((feature) => (
                          <li key={feature} className="flex items-center gap-1.5 text-xs">
                            <Check className="w-3 h-3 text-secondary-500 shrink-0" />
                            <span className="text-dark-300">{feature}</span>
                          </li>
                        ))}
                      </ul>
                    </motion.div>
                  )}
                </div>

                <div
                  className={`w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 mt-0.5 transition-colors ${
                    isSelected ? 'border-primary-500 bg-primary-500' : 'border-dark-600'
                  }`}
                >
                  {isSelected && <Check className="w-3 h-3 text-white" />}
                </div>
              </div>
            </motion.button>
          );
        })}
      </div>

      <button
        type="button"
        onClick={onContinue}
        disabled={!selectedPlan}
        className="btn-primary w-full mt-6"
      >
        Continue
        <ArrowRight className="w-4 h-4" />
      </button>

      <div className="mt-4 text-center">
        <Link
          to="/pricing"
          className="text-xs text-dark-400 hover:text-dark-300 underline underline-offset-2"
        >
          Compare plans in detail
        </Link>
      </div>
    </motion.div>
  );
}
