import { AlertCircle } from 'lucide-react';

import {
  BILL_CATEGORY_LABELS,
  type Biller,
} from '../../../api/billPaymentApi';
import { CATEGORY_COLORS, getIcon, formatCurrency } from './constants';

interface PaymentFormProps {
  biller: Biller;
  accountNumber: string;
  accountHolderName: string;
  amount: string;
  description: string;
  availableBalance: number | undefined;
  error: string | null;
  onAccountNumberChange: (value: string) => void;
  onAccountHolderNameChange: (value: string) => void;
  onAmountChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onDescriptionChange: (value: string) => void;
  onSubmit: () => void;
}

export default function PaymentForm({
  biller,
  accountNumber,
  accountHolderName,
  amount,
  description,
  availableBalance,
  error,
  onAccountNumberChange,
  onAccountHolderNameChange,
  onAmountChange,
  onDescriptionChange,
  onSubmit,
}: PaymentFormProps) {
  const Icon = getIcon(biller.icon);

  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(); }} className="space-y-4">
      {/* Selected biller badge */}
      <div className="flex items-center gap-3 p-3 bg-dark-800/50 rounded-xl border border-dark-700/50">
        <div
          className={`w-10 h-10 ${CATEGORY_COLORS[biller.category]} rounded-full flex items-center justify-center`}
        >
          <Icon className="w-5 h-5 text-white" />
        </div>
        <div>
          <p className="text-sm font-medium text-white">{biller.name}</p>
          <p className="text-xs text-dark-400">
            {BILL_CATEGORY_LABELS[biller.category]}
          </p>
        </div>
      </div>

      {/* Account number */}
      <div>
        <label className="block text-sm font-medium text-dark-300 mb-1.5">
          Account / Reference Number
        </label>
        <input
          type="text"
          value={accountNumber}
          onChange={(e) => onAccountNumberChange(e.target.value)}
          placeholder="Enter your account number"
          className="w-full px-4 py-2.5 bg-dark-800 border border-dark-700 rounded-lg text-white placeholder-dark-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500/50 text-sm"
          required
        />
      </div>

      {/* Account holder name (optional) */}
      <div>
        <label className="block text-sm font-medium text-dark-300 mb-1.5">
          Account Holder Name <span className="text-dark-500">(optional)</span>
        </label>
        <input
          type="text"
          value={accountHolderName}
          onChange={(e) => onAccountHolderNameChange(e.target.value)}
          placeholder="Name on the account"
          className="w-full px-4 py-2.5 bg-dark-800 border border-dark-700 rounded-lg text-white placeholder-dark-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500/50 text-sm"
        />
      </div>

      {/* Amount */}
      <div>
        <label className="block text-sm font-medium text-dark-300 mb-1.5">
          Amount
        </label>
        <div className="relative">
          <span className="absolute left-4 top-1/2 -translate-y-1/2 text-dark-400 text-sm">
            $
          </span>
          <input
            type="text"
            inputMode="decimal"
            value={amount}
            onChange={onAmountChange}
            placeholder="0.00"
            className="w-full pl-8 pr-4 py-2.5 bg-dark-800 border border-dark-700 rounded-lg text-white placeholder-dark-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500/50 text-sm"
            required
          />
        </div>
        {availableBalance != null && (
          <p className="mt-1 text-xs text-dark-400">
            Available: {formatCurrency(availableBalance)}
          </p>
        )}
      </div>

      {/* Description */}
      <div>
        <label className="block text-sm font-medium text-dark-300 mb-1.5">
          Note <span className="text-dark-500">(optional)</span>
        </label>
        <input
          type="text"
          value={description}
          onChange={(e) => onDescriptionChange(e.target.value)}
          placeholder="e.g. January bill"
          className="w-full px-4 py-2.5 bg-dark-800 border border-dark-700 rounded-lg text-white placeholder-dark-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500/50 text-sm"
          maxLength={120}
        />
      </div>

      {error && (
        <div className="flex items-center gap-2 text-red-400 text-sm bg-red-500/10 px-3 py-2 rounded-lg">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <button
        type="submit"
        className="w-full py-3 bg-purple-600 hover:bg-purple-700 text-white font-semibold rounded-xl transition-colors"
      >
        Review Payment
      </button>
    </form>
  );
}
