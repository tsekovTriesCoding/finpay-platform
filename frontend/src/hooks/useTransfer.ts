import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { paymentService, MoneyTransfer, MoneyTransferRequest } from '../api';
import { walletKeys } from './useWallet';

export const transferKeys = {
  all: ['transfers'] as const,
  user: (userId: string) => [...transferKeys.all, 'user', userId] as const,
  detail: (transferId: string) => [...transferKeys.all, 'detail', transferId] as const,
};

/**
 * Fetch recent transfer history for a user.
 */
export function useTransferHistory(userId: string | undefined, page = 0, size = 5) {
  return useQuery({
    queryKey: [...transferKeys.user(userId!), page, size],
    queryFn: () => paymentService.getTransferHistory(userId!, page, size),
    enabled: !!userId,
  });
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
