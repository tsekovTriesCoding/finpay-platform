import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Check,
  X,
  Loader2,
  ArrowDownLeft,
  Clock,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

import { MoneyRequest } from '../../api';
import {
  usePendingIncomingRequests,
  useApproveMoneyRequest,
  useDeclineMoneyRequest,
  usePendingRequestCount,
} from '../../hooks';

interface PendingRequestsPanelProps {
  userId: string;
  onRequestActioned?: () => void;
}

export default function PendingRequestsPanel({
  userId,
  onRequestActioned,
}: PendingRequestsPanelProps) {
  const [expanded, setExpanded] = useState(true);
  const [processingId, setProcessingId] = useState<string | null>(null);

  const { data: pendingData, refetch } = usePendingIncomingRequests(userId);
  const { data: pendingCount } = usePendingRequestCount(userId);
  const approveMutation = useApproveMoneyRequest(userId);
  const declineMutation = useDeclineMoneyRequest(userId);

  const pendingRequests = pendingData?.content ?? [];
  const count = pendingCount ?? pendingRequests.length;

  if (count === 0) return null;

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);

  const formatTimeAgo = (dateStr: string) => {
    const diff = Date.now() - new Date(dateStr).getTime();
    const minutes = Math.floor(diff / 60_000);
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  };

  const handleApprove = (request: MoneyRequest) => {
    setProcessingId(request.id);
    approveMutation.mutate(request.id, {
      onSettled: () => {
        setProcessingId(null);
        refetch();
        onRequestActioned?.();
      },
    });
  };

  const handleDecline = (request: MoneyRequest) => {
    setProcessingId(request.id);
    declineMutation.mutate(request.id, {
      onSettled: () => {
        setProcessingId(null);
        refetch();
        onRequestActioned?.();
      },
    });
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="card p-6 mb-8 border-green-500/30"
    >
      {/* Header */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex items-center justify-between w-full text-left"
      >
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-green-500/20 rounded-full flex items-center justify-center">
            <ArrowDownLeft className="w-4 h-4 text-green-400" />
          </div>
          <h2 className="text-lg font-semibold text-white">Pending Requests</h2>
          <span className="px-2.5 py-0.5 bg-green-500/20 text-green-400 rounded-full text-sm font-medium">
            {count}
          </span>
        </div>
        {expanded ? (
          <ChevronUp className="w-5 h-5 text-dark-400" />
        ) : (
          <ChevronDown className="w-5 h-5 text-dark-400" />
        )}
      </button>

      {/* Request list */}
      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="mt-4 space-y-3">
              {pendingRequests.map((req) => {
                const isProcessing = processingId === req.id;

                return (
                  <div
                    key={req.id}
                    className="flex items-center justify-between p-4 bg-dark-800/50 rounded-xl border border-dark-700/50"
                  >
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <p className="font-medium text-white truncate">
                          {formatCurrency(req.amount)}{' '}
                          <span className="text-dark-400 font-normal">requested</span>
                        </p>
                      </div>
                      {req.description && (
                        <p className="text-sm text-dark-400 truncate">{req.description}</p>
                      )}
                      <div className="flex items-center gap-1 mt-1">
                        <Clock className="w-3 h-3 text-dark-500" />
                        <span className="text-xs text-dark-500">
                          {formatTimeAgo(req.createdAt)}
                        </span>
                      </div>
                    </div>

                    {/* Action buttons */}
                    <div className="flex items-center gap-2 ml-4">
                      {isProcessing ? (
                        <Loader2 className="w-5 h-5 text-dark-400 animate-spin" />
                      ) : (
                        <>
                          <button
                            onClick={() => handleDecline(req)}
                            className="p-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-lg transition-colors"
                            title="Decline"
                          >
                            <X className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleApprove(req)}
                            className="p-2 bg-green-500/10 hover:bg-green-500/20 text-green-400 rounded-lg transition-colors"
                            title="Approve & Pay"
                          >
                            <Check className="w-4 h-4" />
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
