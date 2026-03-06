import { useState } from 'react';
import { motion } from 'framer-motion';
import { Loader2 } from 'lucide-react';

/* Public type */
export interface ConfirmAction {
  label: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  iconColor: string;
  btnColor: string;
  /** Called when the user confirms. Must invoke onSuccess / onError to dismiss / reset the modal. */
  onConfirm: (callbacks: { onSuccess: () => void; onError: () => void }) => void;
}

/* Component */
export default function ConfirmModal({
  action,
  onClose,
}: {
  action: ConfirmAction;
  onClose: () => void;
}) {
  const Icon = action.icon;
  const [pending, setPending] = useState(false);

  const handleConfirm = () => {
    setPending(true);
    action.onConfirm({
      onSuccess: () => onClose(),
      onError: () => setPending(false),
    });
  };

  return (
    <motion.div
      className="fixed inset-0 z-[60] flex items-center justify-center p-4"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      <motion.div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={!pending ? onClose : undefined}
      />
      <motion.div
        className="relative w-full max-w-sm bg-dark-800 border border-dark-700 rounded-2xl shadow-2xl overflow-hidden"
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        transition={{ duration: 0.2 }}
      >
        <div className="p-6 text-center space-y-4">
          <div className={`mx-auto w-12 h-12 rounded-full flex items-center justify-center ${action.iconColor} bg-opacity-20`}>
            <Icon className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white">{action.label}</h3>
            <p className="text-sm text-gray-400 mt-1">{action.description}</p>
          </div>
          <div className="flex gap-3 pt-2">
            <button
              onClick={onClose}
              disabled={pending}
              className="flex-1 px-4 py-2.5 text-sm font-medium text-gray-300 bg-dark-700 hover:bg-dark-600
                         rounded-xl transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirm}
              disabled={pending}
              className={`flex-1 px-4 py-2.5 text-sm font-medium text-white rounded-xl transition-colors
                         disabled:opacity-50 flex items-center justify-center gap-2 ${action.btnColor}`}
            >
              {pending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Processing…
                </>
              ) : (
                'Confirm'
              )}
            </button>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
