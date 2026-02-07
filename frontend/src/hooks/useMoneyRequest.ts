import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import {
  paymentService,
  MoneyRequest,
  MoneyRequestCreatePayload,
} from '../api';
import { walletKeys } from './useWallet';
import { transferKeys } from './useTransfer';

export const moneyRequestKeys = {
  all: ['moneyRequests'] as const,
  user: (userId: string) => [...moneyRequestKeys.all, 'user', userId] as const,
  pendingIncoming: (userId: string) =>
    [...moneyRequestKeys.all, 'pendingIncoming', userId] as const,
  pendingOutgoing: (userId: string) =>
    [...moneyRequestKeys.all, 'pendingOutgoing', userId] as const,
  pendingCount: (userId: string) =>
    [...moneyRequestKeys.all, 'pendingCount', userId] as const,
  detail: (requestId: string) =>
    [...moneyRequestKeys.all, 'detail', requestId] as const,
};

/**
 * Fetch all money requests for a user (as requester or payer).
 */
export function useMoneyRequests(userId: string | undefined, page = 0, size = 10) {
  return useQuery({
    queryKey: [...moneyRequestKeys.user(userId!), page, size],
    queryFn: () => paymentService.getMoneyRequests(userId!, page, size),
    enabled: !!userId,
  });
}

/**
 * Pending incoming requests (user is the payer - needs to approve/decline).
 */
export function usePendingIncomingRequests(userId: string | undefined, page = 0, size = 10) {
  return useQuery({
    queryKey: [...moneyRequestKeys.pendingIncoming(userId!), page, size],
    queryFn: () => paymentService.getPendingIncomingRequests(userId!, page, size),
    enabled: !!userId,
    refetchInterval: 30_000, // poll every 30s for new incoming requests
  });
}

/**
 * Pending outgoing requests (user is the requester — waiting for approval).
 */
export function usePendingOutgoingRequests(userId: string | undefined, page = 0, size = 10) {
  return useQuery({
    queryKey: [...moneyRequestKeys.pendingOutgoing(userId!), page, size],
    queryFn: () => paymentService.getPendingOutgoingRequests(userId!, page, size),
    enabled: !!userId,
  });
}

/**
 * Badge count of pending incoming requests.
 */
export function usePendingRequestCount(userId: string | undefined) {
  return useQuery({
    queryKey: moneyRequestKeys.pendingCount(userId!),
    queryFn: () => paymentService.getPendingRequestCount(userId!),
    enabled: !!userId,
    refetchInterval: 30_000,
  });
}

// ─── Mutations ────────────────────────────────────────────────

/**
 * Create a money request.
 */
export function useCreateMoneyRequest(requesterUserId: string) {
  const queryClient = useQueryClient();

  return useMutation<MoneyRequest, Error, MoneyRequestCreatePayload>({
    mutationFn: (payload) =>
      paymentService.createMoneyRequest(requesterUserId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: moneyRequestKeys.all });
    },
  });
}

/**
 * Approve a money request (user = payer).
 */
export function useApproveMoneyRequest(payerUserId: string) {
  const queryClient = useQueryClient();

  return useMutation<MoneyRequest, Error, string>({
    mutationFn: (requestId) =>
      paymentService.approveMoneyRequest(payerUserId, requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: moneyRequestKeys.all });
      queryClient.invalidateQueries({ queryKey: walletKeys.user(payerUserId) });
      queryClient.invalidateQueries({ queryKey: transferKeys.user(payerUserId) });
    },
  });
}

/**
 * Decline a money request (user = payer).
 */
export function useDeclineMoneyRequest(payerUserId: string) {
  const queryClient = useQueryClient();

  return useMutation<MoneyRequest, Error, string>({
    mutationFn: (requestId) =>
      paymentService.declineMoneyRequest(payerUserId, requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: moneyRequestKeys.all });
    },
  });
}

/**
 * Cancel a money request (user = requester).
 */
export function useCancelMoneyRequest(requesterUserId: string) {
  const queryClient = useQueryClient();

  return useMutation<MoneyRequest, Error, string>({
    mutationFn: (requestId) =>
      paymentService.cancelMoneyRequest(requesterUserId, requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: moneyRequestKeys.all });
    },
  });
}
