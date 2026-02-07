import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

import { paymentService, MoneyTransfer, MoneyTransferRequest } from '../api';
import { walletKeys } from './useWallet';

export const transferKeys = {
  all: ['transfers'] as const,
  user: (userId: string) => [...transferKeys.all, 'user', userId] as const,
  detail: (transferId: string) => [...transferKeys.all, 'detail', transferId] as const,
};

/**
 * Fetch recent transfer history for a user.
 * Smart polling: only refetches every 5 s while there are PROCESSING transfers.
 * Once all transfers settle (COMPLETED / FAILED), polling stops - zero overhead.
 * Automatically refreshes the wallet balance when a transfer completes.
 */
export function useTransferHistory(userId: string | undefined, page = 0, size = 5) {
  const queryClient = useQueryClient();
  const prevActiveRef = useRef(false);

  const query = useQuery({
    queryKey: [...transferKeys.user(userId!), page, size],
    queryFn: () => paymentService.getTransferHistory(userId!, page, size),
    enabled: !!userId,
    refetchInterval: (q) => {
      const hasActive = q.state.data?.content?.some(
        (t: MoneyTransfer) => t.status === 'PROCESSING' || t.status === 'PENDING'
      );
      return hasActive ? 5_000 : false;
    },
  });

  // When transfers transition from active → settled, refresh the wallet balance
  const hasActive = query.data?.content?.some(
    (t) => t.status === 'PROCESSING' || t.status === 'PENDING'
  ) ?? false;

  useEffect(() => {
    if (prevActiveRef.current && !hasActive && userId) {
      queryClient.invalidateQueries({ queryKey: walletKeys.user(userId) });
    }
    prevActiveRef.current = hasActive;
  }, [hasActive, userId, queryClient]);

  return query;
}

/**
 * Send money mutation.
 * On success, invalidates both wallet and transfer-history queries
 * so they re-fetch automatically.
 */
export function useSendMoney(senderUserId: string) {
  const queryClient = useQueryClient();

  return useMutation<MoneyTransfer, Error, MoneyTransferRequest>({
    mutationFn: (request) => paymentService.sendMoney(senderUserId, request),
    onSuccess: () => {
      // Invalidate wallet + transfers so dashboard refreshes automatically
      queryClient.invalidateQueries({ queryKey: walletKeys.user(senderUserId) });
      queryClient.invalidateQueries({ queryKey: transferKeys.user(senderUserId) });
    },
  });
}
