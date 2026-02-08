import { CheckCircle2 } from 'lucide-react';

import type { BillPayment } from '../../../api/billPaymentApi';
import { formatCurrency } from './constants';

interface SuccessStateProps {
  payment: BillPayment;
  onClose: () => void;
}

export default function SuccessState({ payment, onClose }: SuccessStateProps) {
  return (
    <div className="text-center py-6">
      <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
        <CheckCircle2 className="w-8 h-8 text-green-400" />
      </div>
      <p className="text-white font-medium text-lg mb-1">Payment Submitted!</p>
      <p className="text-dark-400 text-sm mb-6">
        Your bill payment is being processed.
      </p>

      <div className="bg-dark-800/50 rounded-xl p-4 space-y-2 text-sm border border-dark-700/50 mb-6">
        <div className="flex justify-between">
          <span className="text-dark-400">Biller</span>
          <span className="text-white">{payment.billerName}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-dark-400">Amount</span>
          <span className="text-white">{formatCurrency(payment.amount)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-dark-400">Total</span>
          <span className="text-purple-400 font-medium">
            {formatCurrency(payment.totalAmount)}
          </span>
        </div>
        <div className="flex justify-between">
          <span className="text-dark-400">Reference</span>
          <span className="text-white font-mono text-xs">
            {payment.transactionReference}
          </span>
        </div>
        <div className="flex justify-between">
          <span className="text-dark-400">Status</span>
          <span
            className={`font-medium px-2 py-0.5 rounded-full text-xs ${
              payment.status === 'COMPLETED'
                ? 'bg-green-500/20 text-green-400'
                : 'bg-yellow-500/20 text-yellow-400'
            }`}
          >
            {payment.status}
          </span>
        </div>
      </div>

      <button
        onClick={onClose}
        className="w-full py-3 bg-purple-600 hover:bg-purple-700 text-white font-semibold rounded-xl transition-colors"
      >
        Done
      </button>
    </div>
  );
}
