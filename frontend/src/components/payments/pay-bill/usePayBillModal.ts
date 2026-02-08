import { useState, useEffect, useMemo, useCallback } from 'react';

import {
  BILLERS,
  BILL_CATEGORY_LABELS,
  type BillCategory,
  type BillPayment,
  type Biller,
} from '../../../api/billPaymentApi';
import { useWallet } from '../../../hooks/useWallet';
import { usePayBill } from '../../../hooks/useBillPayment';
import type { PayBillStep } from './constants';

interface UsePayBillModalOptions {
  isOpen: boolean;
  userId: string;
  onPaymentComplete?: (payment: BillPayment) => void;
}

export default function usePayBillModal({
  isOpen,
  userId,
  onPaymentComplete,
}: UsePayBillModalOptions) {
  const [step, setStep] = useState<PayBillStep>('category');
  const [selectedCategory, setSelectedCategory] = useState<BillCategory | null>(null);
  const [selectedBiller, setSelectedBiller] = useState<Biller | null>(null);
  const [accountNumber, setAccountNumber] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [payment, setPayment] = useState<BillPayment | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const { data: wallet } = useWallet(isOpen ? userId : undefined);
  const payBill = usePayBill(userId);

  // Reset state when modal closes
  useEffect(() => {
    if (!isOpen) {
      const timeout = setTimeout(() => {
        setStep('category');
        setSelectedCategory(null);
        setSelectedBiller(null);
        setAccountNumber('');
        setAccountHolderName('');
        setAmount('');
        setDescription('');
        setPayment(null);
        setError(null);
        setSearchQuery('');
      }, 300);
      return () => clearTimeout(timeout);
    }
  }, [isOpen]);

  // Filter billers by selected category and search query
  const filteredBillers = useMemo(() => {
    let billers = BILLERS;
    if (selectedCategory) {
      billers = billers.filter((b) => b.category === selectedCategory);
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      billers = billers.filter(
        (b) => b.name.toLowerCase().includes(q) || b.code.toLowerCase().includes(q),
      );
    }
    return billers;
  }, [selectedCategory, searchQuery]);

  const handleCategorySelect = useCallback((cat: BillCategory) => {
    setSelectedCategory(cat);
    setStep('biller');
    setSearchQuery('');
  }, []);

  const handleBillerSelect = useCallback((biller: Biller) => {
    setSelectedBiller(biller);
    setStep('form');
  }, []);

  const handleAmountChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    if (/^\d*\.?\d{0,2}$/.test(value) || value === '') {
      setAmount(value);
      setError(null);
    }
  }, []);

  const handleAccountNumberChange = useCallback((value: string) => {
    setAccountNumber(value);
    setError(null);
  }, []);

  const validateForm = useCallback((): boolean => {
    if (!selectedBiller) {
      setError('Please select a biller');
      return false;
    }
    if (!accountNumber.trim()) {
      setError('Please enter your account number');
      return false;
    }
    const num = parseFloat(amount);
    if (!amount || isNaN(num) || num <= 0) {
      setError('Please enter a valid amount');
      return false;
    }
    if (wallet && num > wallet.availableBalance) {
      setError(`Insufficient funds. Available: $${wallet.availableBalance.toFixed(2)}`);
      return false;
    }
    return true;
  }, [selectedBiller, accountNumber, amount, wallet]);

  const handleSubmit = useCallback(() => {
    if (!validateForm()) return;
    setStep('confirming');
  }, [validateForm]);

  const handleConfirm = useCallback(() => {
    if (!selectedBiller) return;

    setStep('processing');
    setError(null);

    payBill.mutate(
      {
        userId,
        category: selectedBiller.category,
        billerName: selectedBiller.name,
        billerCode: selectedBiller.code,
        accountNumber: accountNumber.trim(),
        accountHolderName: accountHolderName.trim() || undefined,
        amount: parseFloat(amount),
        currency: 'USD',
        description: description.trim() || undefined,
      },
      {
        onSuccess: (result) => {
          setPayment(result);
          if (result.status === 'FAILED' || result.status === 'COMPENSATED') {
            setError(result.failureReason || 'Payment failed');
            setStep('error');
          } else {
            setStep('success');
            onPaymentComplete?.(result);
          }
        },
        onError: (err) => {
          const apiError = (err as { response?: { data?: { message?: string } } })?.response
            ?.data?.message;
          setError(apiError || err.message || 'Payment failed. Please try again.');
          setStep('error');
        },
      },
    );
  }, [selectedBiller, userId, accountNumber, accountHolderName, amount, description, payBill, onPaymentComplete]);

  const handleBack = useCallback(() => {
    if (step === 'biller') {
      setStep('category');
      setSelectedCategory(null);
      setSearchQuery('');
    } else if (step === 'form') {
      setStep('biller');
      setSelectedBiller(null);
    } else if (step === 'confirming') {
      setStep('form');
    }
  }, [step]);

  const handleTryAgain = useCallback(() => {
    setStep('form');
    setError(null);
  }, []);

  const headerTitle = (() => {
    switch (step) {
      case 'category':
        return 'Pay Bills';
      case 'biller':
        return BILL_CATEGORY_LABELS[selectedCategory!] ?? 'Select Biller';
      case 'form':
        return selectedBiller?.name ?? 'Payment Details';
      case 'confirming':
        return 'Confirm Payment';
      case 'processing':
        return 'Processing...';
      case 'success':
        return 'Payment Complete';
      case 'error':
        return 'Payment Failed';
    }
  })();

  return {
    // State
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
    availableBalance: wallet?.availableBalance,

    // Handlers
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
  };
}
