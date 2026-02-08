import { AlertTriangle, RefreshCw } from 'lucide-react';

interface SectionErrorProps {
  message?: string;
  onRetry?: () => void;
}

/** Inline error state for a dashboard section. */
export default function SectionError({
  message = 'Failed to load data',
  onRetry,
}: SectionErrorProps) {
  return (
    <div className="card p-6 mb-8">
      <div className="flex flex-col items-center justify-center py-8 text-center">
        <div className="w-12 h-12 bg-red-500/10 rounded-full flex items-center justify-center mb-3">
          <AlertTriangle className="w-6 h-6 text-red-400" />
        </div>
        <p className="text-sm font-medium text-dark-300">{message}</p>
        {onRetry && (
          <button
            onClick={onRetry}
            className="mt-3 flex items-center gap-1.5 text-xs text-primary-400 hover:text-primary-300 transition-colors"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Try again
          </button>
        )}
      </div>
    </div>
  );
}
