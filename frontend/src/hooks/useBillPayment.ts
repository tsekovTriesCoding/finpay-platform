import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { billPaymentService, BillPayment, BillPaymentRequest, BillPaymentPageResponse } from '../api/billPaymentApi';
import { walletKeys } from './useWallet';

export const billPaymentKeys = {
  all: ['billPayments'] as const,
  user: (userId: string) => [...billPaymentKeys.all, 'user', userId] as const,
  detail: (id: string) => [...billPaymentKeys.all, 'detail', id] as const,
};

/**
 * Fetch paginated bill payment history for a user.
 * Smart polling: polls every 5s while any bill is still PROCESSING/PENDING.
 */
export function useBillPayments(userId: string | undefined, page = 0, size = 10) {
  return useQuery<BillPaymentPageResponse>({
    queryKey: [...billPaymentKeys.user(userId!), page, size],
    queryFn: () => billPaymentService.getUserBillPayments(userId!, page, size),
    enabled: !!userId,
    refetchInterval: (q) => {
      const hasActive = q.state.data?.content?.some(
        (b: BillPayment) => b.status === 'PROCESSING' || b.status === 'PENDING',
      );
      return hasActive ? 5_000 : false;
    },
  });
}

/**
 * Pay a bill. Invalidates wallet + bill history on success.
 */
export function usePayBill(userId: string) {
  const queryClient = useQueryClient();

  return useMutation<BillPayment, Error, BillPaymentRequest>({
    mutationFn: (request) => billPaymentService.payBill(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: walletKeys.user(userId) });
      queryClient.invalidateQueries({ queryKey: billPaymentKeys.user(userId) });
    },
  });
}

/**
 * Cancel a pending bill payment.
 */
export function useCancelBillPayment(userId: string) {
  const queryClient = useQueryClient();

  return useMutation<BillPayment, Error, string>({
    mutationFn: (billId) => billPaymentService.cancelBillPayment(billId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: walletKeys.user(userId) });
      queryClient.invalidateQueries({ queryKey: billPaymentKeys.user(userId) });
    },
  });
}
