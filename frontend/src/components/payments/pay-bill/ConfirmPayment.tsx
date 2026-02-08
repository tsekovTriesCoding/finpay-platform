import { BILL_CATEGORY_LABELS, type Biller } from '../../../api/billPaymentApi';
import { formatCurrency } from './constants';

interface ConfirmPaymentProps {
  biller: Biller;
  accountNumber: string;
  accountHolderName: string;
  amount: number;
  description: string;
  onBack: () => void;
  onConfirm: () => void;
}

export default function ConfirmPayment({
  biller,
  accountNumber,
  accountHolderName,
  amount,
  description,
  onBack,
  onConfirm,
}: ConfirmPaymentProps) {
  const fee = Math.max(amount * 0.005, 0.25);
  const total = amount + fee;

  return (
    <div className="space-y-4">
      <div className="bg-dark-800/50 rounded-xl p-4 space-y-3 border border-dark-700/50">
        <div className="flex justify-between text-sm">
          <span className="text-dark-400">Biller</span>
          <span className="text-white font-medium">{biller.name}</span>
        </div>
        <div className="flex justify-between text-sm">
          <span className="text-dark-400">Category</span>
          <span className="text-white">
            {BILL_CATEGORY_LABELS[biller.category]}
          </span>
        </div>
        <div className="flex justify-between text-sm">
          <span className="text-dark-400">Account</span>
          <span className="text-white font-mono">{accountNumber}</span>
        </div>
        {accountHolderName && (
          <div className="flex justify-between text-sm">
            <span className="text-dark-400">Name</span>
            <span className="text-white">{accountHolderName}</span>
          </div>
        )}
        <hr className="border-dark-700/50" />
        <div className="flex justify-between text-sm">
          <span className="text-dark-400">Amount</span>
          <span className="text-white font-medium">{formatCurrency(amount)}</span>
        </div>
        <div className="flex justify-between text-sm">
          <span className="text-dark-400">Processing Fee (0.5%)</span>
          <span className="text-dark-300">{formatCurrency(fee)}</span>
        </div>
        <hr className="border-dark-700/50" />
        <div className="flex justify-between text-base font-semibold">
          <span className="text-white">Total</span>
          <span className="text-purple-400">{formatCurrency(total)}</span>
        </div>
      </div>

      {description && (
        <div className="bg-dark-800/50 rounded-xl p-3 border border-dark-700/50">
          <p className="text-xs text-dark-400 mb-1">Note</p>
          <p className="text-sm text-dark-200">{description}</p>
        </div>
      )}

      <div className="flex gap-3">
        <button
          onClick={onBack}
          className="flex-1 py-3 bg-dark-800 hover:bg-dark-700 text-white font-medium rounded-xl transition-colors border border-dark-700"
        >
          Edit
        </button>
        <button
          onClick={onConfirm}
          className="flex-1 py-3 bg-purple-600 hover:bg-purple-700 text-white font-semibold rounded-xl transition-colors"
        >
          Pay Now
        </button>
      </div>
    </div>
  );
}
