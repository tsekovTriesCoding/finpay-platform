import { XCircle } from 'lucide-react';

interface ErrorStateProps {
  error: string | null;
  onClose: () => void;
  onTryAgain: () => void;
}

export default function ErrorState({ error, onClose, onTryAgain }: ErrorStateProps) {
  return (
    <div className="text-center py-6">
      <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
        <XCircle className="w-8 h-8 text-red-400" />
      </div>
      <p className="text-white font-medium text-lg mb-1">Payment Failed</p>
      <p className="text-dark-400 text-sm mb-6">
        {error || 'Something went wrong with your payment.'}
      </p>

      <div className="flex gap-3">
        <button
          onClick={onClose}
          className="flex-1 py-3 bg-dark-800 hover:bg-dark-700 text-white font-medium rounded-xl transition-colors border border-dark-700"
        >
          Close
        </button>
        <button
          onClick={onTryAgain}
          className="flex-1 py-3 bg-purple-600 hover:bg-purple-700 text-white font-semibold rounded-xl transition-colors"
        >
          Try Again
        </button>
      </div>
    </div>
  );
}
