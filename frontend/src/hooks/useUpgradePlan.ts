import { useMutation, useQueryClient } from '@tanstack/react-query';

import { authService, type AccountPlan, type UpgradePlanResponse } from '../api/authApi';
import { useAuth } from '../contexts/AuthContext';
import { walletKeys } from './useWallet';

/**
 * Mutation hook for upgrading the user's plan.
 *
 * Colocates all post-upgrade side effects:
 * - Patches auth context immediately from the API response (avoids Kafka lag)
 * - Invalidates wallet queries so limits/features refetch once the event propagates
 */
export function useUpgradePlan() {
  const queryClient = useQueryClient();
  const { patchUser } = useAuth();

  return useMutation<UpgradePlanResponse, Error, AccountPlan>({
    mutationFn: (newPlan) => authService.upgradePlan({ newPlan }),
    onSuccess: (data) => {
      // Immediately update the plan from the response - no network round-trip,
      // no dependency on Kafka propagation to user-service
      patchUser({ plan: data.newPlan });
      queryClient.invalidateQueries({ queryKey: walletKeys.all });
    },
  });
}
