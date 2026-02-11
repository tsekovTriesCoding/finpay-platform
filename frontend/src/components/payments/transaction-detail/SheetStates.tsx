import { XCircle } from 'lucide-react';

export function SheetSkeleton() {
  return (
    <div className="space-y-6 pt-6 animate-pulse">
      <div className="flex flex-col items-center gap-3">
        <div className="w-16 h-16 rounded-full bg-dark-800" />
        <div className="h-8 w-32 rounded-lg bg-dark-800" />
        <div className="h-4 w-24 rounded bg-dark-800" />
        <div className="h-6 w-20 rounded-full bg-dark-800" />
      </div>
      <div className="h-48 rounded-2xl bg-dark-800/60" />
      <div className="h-40 rounded-2xl bg-dark-800/60" />
    </div>
  );
}

export function SheetError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="py-12 text-center">
      <XCircle className="w-10 h-10 text-red-400 mx-auto mb-3" />
      <p className="text-white font-semibold mb-1">Failed to load details</p>
      <p className="text-dark-400 text-sm mb-4">Something went wrong. Please try again.</p>
      <button
        onClick={onRetry}
        className="px-4 py-2 rounded-lg bg-primary-500 hover:bg-primary-600 text-white text-sm font-medium transition-colors"
      >
        Retry
      </button>
    </div>
  );
}
