import { motion, AnimatePresence } from 'framer-motion';
import { X, ChevronLeft, CreditCard } from 'lucide-react';

import type { BillPayment } from '../../../api/billPaymentApi';

import usePayBillModal from './usePayBillModal';
import CategoryGrid from './CategoryGrid';
import BillerList from './BillerList';
import PaymentForm from './PaymentForm';
import ConfirmPayment from './ConfirmPayment';
import ProcessingState from './ProcessingState';
import SuccessState from './SuccessState';
import ErrorState from './ErrorState';

interface PayBillModalProps {
  isOpen: boolean;
  onClose: () => void;
  userId: string;
  onPaymentComplete?: (payment: BillPayment) => void;
}

export default function PayBillModal({
  isOpen,
  onClose,
  userId,
  onPaymentComplete,
}: PayBillModalProps) {
  const {
    step,
    selectedBiller,
    accountNumber,
    accountHolderName,
    amount,
    description,
    payment,
    error,
    searchQuery,
    filteredBillers,
    headerTitle,
    availableBalance,
    handleCategorySelect,
    handleBillerSelect,
    handleAmountChange,
    handleAccountNumberChange,
    setAccountHolderName,
    setDescription,
    setSearchQuery,
    handleSubmit,
    handleConfirm,
    handleBack,
    handleTryAgain,
  } = usePayBillModal({ isOpen, userId, onPaymentComplete });

  const handleClose = () => {
    if (step === 'processing') return;
    onClose();
  };

  const showBackButton = step === 'biller' || step === 'form' || step === 'confirming';

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={handleClose}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50"
          />

          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
          >
            <div
              className="bg-dark-900 rounded-2xl shadow-xl border border-dark-800/50 w-full max-w-md overflow-hidden max-h-[90vh] flex flex-col"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="bg-gradient-to-r from-purple-600 to-purple-500 px-6 py-4 flex items-center justify-between shrink-0">
                <div className="flex items-center gap-3">
                  {showBackButton && (
                    <button
                      onClick={handleBack}
                      className="p-1.5 text-white/80 hover:text-white hover:bg-white/10 rounded-full transition-colors"
                    >
                      <ChevronLeft className="w-5 h-5" />
                    </button>
                  )}
                  <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                    <CreditCard className="w-5 h-5 text-white" />
                  </div>
                  <h2 className="text-xl font-semibold text-white">{headerTitle}</h2>
                </div>
                {step !== 'processing' && (
                  <button
                    onClick={handleClose}
                    className="p-2 text-white/80 hover:text-white hover:bg-white/10 rounded-full transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                )}
              </div>

              <div className="overflow-y-auto flex-1 p-6">
                {step === 'category' && (
                  <CategoryGrid onSelect={handleCategorySelect} />
                )}

                {step === 'biller' && (
                  <BillerList
                    billers={filteredBillers}
                    searchQuery={searchQuery}
                    onSearchChange={setSearchQuery}
                    onSelect={handleBillerSelect}
                  />
                )}

                {step === 'form' && selectedBiller && (
                  <PaymentForm
                    biller={selectedBiller}
                    accountNumber={accountNumber}
                    accountHolderName={accountHolderName}
                    amount={amount}
                    description={description}
                    availableBalance={availableBalance}
                    error={error}
                    onAccountNumberChange={handleAccountNumberChange}
                    onAccountHolderNameChange={setAccountHolderName}
                    onAmountChange={handleAmountChange}
                    onDescriptionChange={setDescription}
                    onSubmit={handleSubmit}
                  />
                )}

                {step === 'confirming' && selectedBiller && (
                  <ConfirmPayment
                    biller={selectedBiller}
                    accountNumber={accountNumber}
                    accountHolderName={accountHolderName}
                    amount={parseFloat(amount)}
                    description={description}
                    onBack={handleBack}
                    onConfirm={handleConfirm}
                  />
                )}

                {step === 'processing' && <ProcessingState />}

                {step === 'success' && payment && (
                  <SuccessState payment={payment} onClose={handleClose} />
                )}

                {step === 'error' && (
                  <ErrorState
                    error={error}
                    onClose={handleClose}
                    onTryAgain={handleTryAgain}
                  />
                )}
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
