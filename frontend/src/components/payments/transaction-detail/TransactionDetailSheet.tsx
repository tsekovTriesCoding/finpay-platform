import { useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X } from 'lucide-react';

import type { TransactionDetail, TransactionType } from '../../../api/transactionDetailApi';
import { useTransactionDetail } from '../../../hooks/useTransactionDetail';
import TransactionHeader from './TransactionHeader';
import ReceiptSection from './ReceiptSection';
import TimelineSection from './TimelineSection';
import ReferenceSection from './ReferenceSection';
import ActionsSection from './ActionsSection';
import { SheetSkeleton, SheetError } from './SheetStates';

// Props 

interface TransactionDetailSheetProps {
  isOpen: boolean;
  onClose: () => void;
  transactionType: TransactionType | null;
  transactionId: string | null;
  /** Current user ID - used to determine sent/received perspective. */
  userId: string;
}

// Main Component

export default function TransactionDetailSheet({
  isOpen,
  onClose,
  transactionType,
  transactionId,
  userId,
}: TransactionDetailSheetProps) {
  const { data: detail, isLoading, isError, refetch } = useTransactionDetail(
    transactionType,
    transactionId,
  );

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose],
  );

  useEffect(() => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen, handleKeyDown]);

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            key="backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm"
            onClick={onClose}
          />

          <motion.div
            key="sheet"
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 30, stiffness: 300 }}
            className="fixed inset-x-0 bottom-0 z-50 max-h-[92vh] overflow-hidden rounded-t-3xl bg-dark-900 border-t border-dark-700 shadow-2xl sm:inset-x-auto sm:left-1/2 sm:-translate-x-1/2 sm:max-w-lg sm:w-full"
          >
            <div className="flex justify-center pt-3 pb-1">
              <div className="w-10 h-1 rounded-full bg-dark-600" />
            </div>

            <button
              onClick={onClose}
              className="absolute top-4 right-4 p-2 rounded-full bg-dark-800 hover:bg-dark-700 text-dark-400 hover:text-white transition-colors"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="overflow-y-auto max-h-[calc(92vh-3rem)] px-6 pb-8">
              {isLoading && <SheetSkeleton />}
              {isError && <SheetError onRetry={refetch} />}
              {detail && <SheetContent detail={detail} userId={userId} />}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}

// Sheet Content (composition of all sections)

function SheetContent({ detail, userId }: { detail: TransactionDetail; userId: string }) {
  const isSent = detail.senderUserId === userId;

  return (
    <div className="space-y-6 pt-2">
      <TransactionHeader detail={detail} isSent={isSent} />
      <ReceiptSection detail={detail} isSent={isSent} />
      <TimelineSection timeline={detail.timeline} />
      <ReferenceSection reference={detail.transactionReference} />
      {detail.availableActions.length > 0 && (
        <ActionsSection actions={detail.availableActions} />
      )}
    </div>
  );
}
