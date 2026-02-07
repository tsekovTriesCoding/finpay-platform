import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { walletService, Wallet } from '../api';

export const walletKeys = {
  all: ['wallets'] as const,
  user: (userId: string) => [...walletKeys.all, userId] as const,
};

/**
 * Fetch the wallet for a user.
 * 
 * Retries up to 5 times on failure - handles the race condition where
 * a newly registered user's wallet hasn't been created yet by the 
 * async Kafka pipeline (user-events -> wallet-service).
 */
export function useWallet(userId: string | undefined) {
  return useQuery<Wallet>({
    queryKey: walletKeys.user(userId!),
    queryFn: () => walletService.getWallet(userId!),
    enabled: !!userId,
    retry: (failureCount, error) => {
      const status = (error as { response?: { status?: number } })?.response?.status;
      // Retry on 404 (wallet not yet created) or 500 (transient), up to 5 times
      if (status === 404 || status === 500) {
        return failureCount < 5;
      }
      return false;
    },
    retryDelay: (attempt) => Math.min(1000 * Math.pow(2, attempt), 16000),
  });
}

/**
 * Deposit funds into a wallet — invalidates the wallet cache on success.
 */
export function useDeposit() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, amount, currency }: { userId: string; amount: number; currency: string }) =>
      walletService.deposit(userId, amount, currency),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: walletKeys.user(variables.userId) });
    },
  });
}
