import { motion } from 'framer-motion';
import {
  ArrowUpRight,
  ArrowDownLeft,
  CreditCard,
  TrendingUp,
  type LucideIcon,
} from 'lucide-react';

interface Action {
  icon: LucideIcon;
  label: string;
  color: string;
  onClick: () => void;
}

interface QuickActionsProps {
  onSendMoney: () => void;
  onRequestMoney: () => void;
  onPayBills: () => void;
}

/** Static quick-action grid - no data fetching, pure presentational. */
export default function QuickActions({
  onSendMoney,
  onRequestMoney,
  onPayBills,
}: QuickActionsProps) {
  const actions: Action[] = [
    { icon: ArrowUpRight, label: 'Send Money', color: 'bg-blue-500', onClick: onSendMoney },
    { icon: ArrowDownLeft, label: 'Request Money', color: 'bg-green-500', onClick: onRequestMoney },
    { icon: CreditCard, label: 'Pay Bills', color: 'bg-purple-500', onClick: onPayBills },
    { icon: TrendingUp, label: 'Investments', color: 'bg-orange-500', onClick: () => {} },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.2 }}
      className="mb-8"
    >
      <h2 className="text-lg font-semibold text-white mb-4">Quick Actions</h2>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {actions.map((action) => (
          <button
            key={action.label}
            onClick={action.onClick}
            className="card p-4 hover:border-dark-700/50 transition-all hover:scale-[1.02] text-center"
          >
            <div
              className={`w-12 h-12 ${action.color} rounded-full flex items-center justify-center mx-auto mb-3 shadow-lg`}
            >
              <action.icon className="w-6 h-6 text-white" />
            </div>
            <p className="text-sm font-medium text-white">{action.label}</p>
          </button>
        ))}
      </div>
    </motion.div>
  );
}
