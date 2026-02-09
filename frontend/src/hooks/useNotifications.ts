import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { notificationService } from '../api/notificationApi';
import type { Notification } from '../api/notificationApi';

export const notificationKeys = {
  all: ['notifications'] as const,
  byUser: (userId: string) => [...notificationKeys.all, 'user', userId] as const,
  unread: (userId: string) => [...notificationKeys.all, 'unread', userId] as const,
  unreadCount: (userId: string) => [...notificationKeys.all, 'unread-count', userId] as const,
  preferences: (userId: string) => [...notificationKeys.all, 'preferences', userId] as const,
};

export function useNotifications(userId: string) {
  return useQuery({
    queryKey: notificationKeys.byUser(userId),
    queryFn: () => notificationService.getByUserId(userId),
    enabled: !!userId,
    staleTime: 30_000,
    refetchOnWindowFocus: true,
  });
}

export function useUnreadNotifications(userId: string) {
  return useQuery({
    queryKey: notificationKeys.unread(userId),
    queryFn: () => notificationService.getUnread(userId),
    enabled: !!userId,
    staleTime: 15_000,
    refetchOnWindowFocus: true,
  });
}

export function useUnreadCount(userId: string) {
  return useQuery({
    queryKey: notificationKeys.unreadCount(userId),
    queryFn: () => notificationService.getUnreadCount(userId),
    enabled: !!userId,
    staleTime: 10_000,
    refetchInterval: 60_000, // Fallback poll if WebSocket is down
    refetchOnWindowFocus: true,
  });
}

export function useMarkAsRead(userId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: string) => notificationService.markAsRead(notificationId),
    onMutate: async (notificationId) => {
      await queryClient.cancelQueries({ queryKey: notificationKeys.byUser(userId) });
      await queryClient.cancelQueries({ queryKey: notificationKeys.unreadCount(userId) });

      const previousNotifications = queryClient.getQueryData<Notification[]>(
        notificationKeys.byUser(userId),
      );

      if (previousNotifications) {
        queryClient.setQueryData<Notification[]>(
          notificationKeys.byUser(userId),
          previousNotifications.map((n) =>
            n.id === notificationId
              ? { ...n, readAt: new Date().toISOString(), status: 'READ' as const }
              : n,
          ),
        );
      }

      const previousCount = queryClient.getQueryData<number>(
        notificationKeys.unreadCount(userId),
      );
      if (previousCount !== undefined && previousCount > 0) {
        queryClient.setQueryData(notificationKeys.unreadCount(userId), previousCount - 1);
      }

      return { previousNotifications, previousCount };
    },
    onError: (_err, _id, context) => {
      if (context?.previousNotifications) {
        queryClient.setQueryData(notificationKeys.byUser(userId), context.previousNotifications);
      }
      if (context?.previousCount !== undefined) {
        queryClient.setQueryData(notificationKeys.unreadCount(userId), context.previousCount);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.byUser(userId) });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unread(userId) });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount(userId) });
    },
  });
}

export function useMarkAllAsRead(userId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationService.markAllAsRead(userId),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: notificationKeys.byUser(userId) });

      const previousNotifications = queryClient.getQueryData<Notification[]>(
        notificationKeys.byUser(userId),
      );

      if (previousNotifications) {
        const now = new Date().toISOString();
        queryClient.setQueryData<Notification[]>(
          notificationKeys.byUser(userId),
          previousNotifications.map((n) =>
            n.readAt ? n : { ...n, readAt: now, status: 'READ' as const },
          ),
        );
      }

      queryClient.setQueryData(notificationKeys.unreadCount(userId), 0);

      return { previousNotifications };
    },
    onError: (_err, _vars, context) => {
      if (context?.previousNotifications) {
        queryClient.setQueryData(notificationKeys.byUser(userId), context.previousNotifications);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.byUser(userId) });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unread(userId) });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount(userId) });
    },
  });
}

export function useDeleteNotification(userId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: string) => notificationService.delete(notificationId),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.byUser(userId) });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unread(userId) });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount(userId) });
    },
  });
}
