import { Loader2 } from 'lucide-react';

export default function ProcessingState() {
  return (
    <div className="text-center py-8">
      <div className="w-16 h-16 bg-purple-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
        <Loader2 className="w-8 h-8 text-purple-400 animate-spin" />
      </div>
      <p className="text-white font-medium mb-1">Processing Payment</p>
      <p className="text-dark-400 text-sm">
        Please wait while we process your bill payment...
      </p>
    </div>
  );
}
