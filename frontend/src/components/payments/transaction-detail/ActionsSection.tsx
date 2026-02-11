import { ArrowUpRight, AlertTriangle, XCircle } from 'lucide-react';

interface ActionsSectionProps {
  actions: string[];
}

export default function ActionsSection({ actions }: ActionsSectionProps) {
  return (
    <div className="space-y-2 pb-2">
      {actions.includes('DISPUTE') && (
        <button className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/20 text-amber-400 font-semibold text-sm transition-colors">
          <AlertTriangle className="w-4 h-4" />
          Dispute Transaction
        </button>
      )}
      {actions.includes('CANCEL') && (
        <button className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 text-red-400 font-semibold text-sm transition-colors">
          <XCircle className="w-4 h-4" />
          Cancel Transaction
        </button>
      )}
      {actions.includes('RETRY') && (
        <button className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-primary-500/10 hover:bg-primary-500/20 border border-primary-500/20 text-primary-400 font-semibold text-sm transition-colors">
          <ArrowUpRight className="w-4 h-4" />
          Retry Transaction
        </button>
      )}
    </div>
  );
}
