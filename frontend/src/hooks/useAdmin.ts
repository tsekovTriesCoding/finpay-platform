import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  adminService,
  type UserFilterParams,
  type TransactionFilterParams,
  type WalletFilterParams,
  type AuditLogFilterParams,
} from '../api/adminApi';

// Query Keys

export const adminKeys = {
  all: ['admin'] as const,
  users: () => [...adminKeys.all, 'users'] as const,
  userList: (params: UserFilterParams) => [...adminKeys.users(), 'list', params] as const,
  usersByRole: (role: string) => [...adminKeys.users(), 'by-role', role] as const,
  dashboardMetrics: () => [...adminKeys.all, 'dashboard-metrics'] as const,
  transactions: () => [...adminKeys.all, 'transactions'] as const,
  transactionList: (params: TransactionFilterParams) => [...adminKeys.transactions(), 'list', params] as const,
  transactionMetrics: () => [...adminKeys.transactions(), 'metrics'] as const,
  wallets: () => [...adminKeys.all, 'wallets'] as const,
  walletList: (params: WalletFilterParams) => [...adminKeys.wallets(), 'list', params] as const,
  walletMetrics: () => [...adminKeys.wallets(), 'metrics'] as const,
  auditLogs: () => [...adminKeys.all, 'audit-logs'] as const,
  auditLogList: (params: AuditLogFilterParams) => [...adminKeys.auditLogs(), 'list', params] as const,
};

// User Management Hooks

export function useAdminUsers(params: UserFilterParams = {}) {
  return useQuery({
    queryKey: adminKeys.userList(params),
    queryFn: () => adminService.listUsers(params),
  });
}

export function useAdminUsersByRole(role: string) {
  return useQuery({
    queryKey: adminKeys.usersByRole(role),
    queryFn: () => adminService.getUsersByRole(role),
  });
}

export function useAdminDashboardMetrics() {
  return useQuery({
    queryKey: adminKeys.dashboardMetrics(),
    queryFn: () => adminService.getDashboardMetrics(),
    refetchInterval: 30000, // Refresh every 30s
  });
}

export function useChangeUserRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: string }) =>
      adminService.changeUserRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.users() });
      queryClient.invalidateQueries({ queryKey: adminKeys.dashboardMetrics() });
    },
  });
}

export function useSuspendUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, reason }: { userId: string; reason?: string }) =>
      adminService.suspendUser(userId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.users() });
      queryClient.invalidateQueries({ queryKey: adminKeys.dashboardMetrics() });
    },
  });
}

export function useUnsuspendUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => adminService.unsuspendUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.users() });
      queryClient.invalidateQueries({ queryKey: adminKeys.dashboardMetrics() });
    },
  });
}

export function useForcePasswordReset() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => adminService.forcePasswordReset(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.users() });
    },
  });
}

// Transaction Monitoring Hooks

export function useAdminTransactions(params: TransactionFilterParams = {}) {
  return useQuery({
    queryKey: adminKeys.transactionList(params),
    queryFn: () => adminService.listTransactions(params),
  });
}

export function useAdminTransactionMetrics() {
  return useQuery({
    queryKey: adminKeys.transactionMetrics(),
    queryFn: () => adminService.getTransactionMetrics(),
    refetchInterval: 30000,
  });
}

// Wallet Management Hooks

export function useAdminWallets(params: WalletFilterParams = {}) {
  return useQuery({
    queryKey: adminKeys.walletList(params),
    queryFn: () => adminService.listWallets(params),
  });
}

export function useAdminWalletMetrics() {
  return useQuery({
    queryKey: adminKeys.walletMetrics(),
    queryFn: () => adminService.getWalletMetrics(),
    refetchInterval: 30000,
  });
}

export function useFreezeWallet() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => adminService.freezeWallet(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.wallets() });
      queryClient.invalidateQueries({ queryKey: adminKeys.walletMetrics() });
    },
  });
}

export function useUnfreezeWallet() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => adminService.unfreezeWallet(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.wallets() });
      queryClient.invalidateQueries({ queryKey: adminKeys.walletMetrics() });
    },
  });
}

// Audit Log Hooks

export function useAdminAuditLogs(params: AuditLogFilterParams = {}) {
  return useQuery({
    queryKey: adminKeys.auditLogList(params),
    queryFn: () => adminService.searchAuditLogs(params),
  });
}
