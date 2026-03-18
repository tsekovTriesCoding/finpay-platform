import api from './axios';

// Types

export interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string | null;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
  role: 'USER' | 'ADMIN' | 'MERCHANT';
  profileImageUrl: string | null;
  address: string | null;
  city: string | null;
  country: string | null;
  postalCode: string | null;
  emailVerified: boolean;
  phoneVerified: boolean;
  plan: 'STARTER' | 'PRO' | 'ENTERPRISE';
  createdAt: string;
  updatedAt: string;
  lastLoginAt: string | null;
}

export interface AdminTransaction {
  id: string;
  type: 'TRANSFER' | 'BILL_PAYMENT' | 'MONEY_REQUEST';
  transactionReference: string;
  fromUserId: string | null;
  toUserId: string | null;
  amount: number;
  currency: string;
  status: string;
  description: string | null;
  flagged: boolean;
  flagReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminWallet {
  id: string;
  userId: string;
  balance: number;
  reservedBalance: number;
  availableBalance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  plan: 'STARTER' | 'PRO' | 'ENTERPRISE';
  dailyTransactionLimit: number;
  monthlyTransactionLimit: number;
  dailySpent: number;
  monthlySpent: number;
  remainingDailyLimit: number;
  remainingMonthlyLimit: number;
  maxVirtualCards: number | null;
  multiCurrencyEnabled: boolean | null;
  apiAccessEnabled: boolean | null;
  createdAt: string;
  updatedAt: string;
}

export interface AuditLogEntry {
  id: string;
  actorId: string;
  actorEmail: string;
  action: string;
  targetType: string;
  targetId: string;
  description: string;
  previousState: string | null;
  newState: string | null;
  ipAddress: string | null;
  serviceSource: string;
  createdAt: string;
}

export interface AdminDashboardMetrics {
  totalUsers: number;
  activeUsers: number;
  suspendedUsers: number;
  pendingVerification: number;
  adminCount: number;
  merchantCount: number;
  regularUserCount: number;
  recentAuditActions24h: number;
  recentAuditActions7d: number;
}

export interface TransactionMetrics {
  totalTransfers: number;
  completedTransfers: number;
  failedTransfers: number;
  pendingTransfers: number;
  totalTransferVolume: number;
  totalBillPayments: number;
  completedBillPayments: number;
  failedBillPayments: number;
  totalBillPaymentVolume: number;
  totalMoneyRequests: number;
  pendingMoneyRequests: number;
  completedMoneyRequests: number;
  declinedMoneyRequests: number;
  flaggedTransactions: number;
}

export interface WalletMetrics {
  totalWallets: number;
  activeWallets: number;
  frozenWallets: number;
  closedWallets: number;
  totalBalance: number;
}

export interface PageResponse<T> {
  content: T[];
  page: {
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  };
}

// Query params

export interface PaginationParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface UserFilterParams extends PaginationParams {
  search?: string;
  status?: string;
  role?: string;
}

export interface TransactionFilterParams extends PaginationParams {
  type?: string;
  status?: string;
}

export interface WalletFilterParams extends PaginationParams {
  status?: string;
}

export interface AuditLogFilterParams extends PaginationParams {
  actorId?: string;
  targetType?: string;
  action?: string;
  startDate?: string;
  endDate?: string;
}

// API Service

export const adminService = {
  // User Management
  listUsers: async (params: UserFilterParams = {}): Promise<PageResponse<AdminUser>> => {
    const { data } = await api.get('/api/v1/admin/users', { params });
    return data;
  },

  changeUserRole: async (userId: string, role: string): Promise<AdminUser> => {
    const { data } = await api.patch(`/api/v1/admin/users/${userId}/role`, { role });
    return data;
  },

  suspendUser: async (userId: string, reason?: string): Promise<AdminUser> => {
    const { data } = await api.post(`/api/v1/admin/users/${userId}/suspend`, { reason });
    return data;
  },

  unsuspendUser: async (userId: string): Promise<AdminUser> => {
    const { data } = await api.post(`/api/v1/admin/users/${userId}/unsuspend`);
    return data;
  },

  forcePasswordReset: async (userId: string): Promise<AdminUser> => {
    const { data } = await api.post(`/api/v1/admin/users/${userId}/force-password-reset`);
    return data;
  },

  getUsersByRole: async (role: string): Promise<AdminUser[]> => {
    const { data } = await api.get(`/api/v1/admin/users/by-role/${role}`);
    return data;
  },

  getDashboardMetrics: async (): Promise<AdminDashboardMetrics> => {
    const { data } = await api.get('/api/v1/admin/users/dashboard/metrics');
    return data;
  },

  // Transaction Monitoring
  listTransactions: async (params: TransactionFilterParams = {}): Promise<PageResponse<AdminTransaction>> => {
    const { data } = await api.get('/api/v1/admin/transactions', { params });
    return data;
  },

  getTransactionMetrics: async (): Promise<TransactionMetrics> => {
    const { data } = await api.get('/api/v1/admin/transactions/metrics');
    return data;
  },

  // Wallet Management
  listWallets: async (params: WalletFilterParams = {}): Promise<PageResponse<AdminWallet>> => {
    const { data } = await api.get('/api/v1/admin/wallets', { params });
    return data;
  },

  freezeWallet: async (userId: string): Promise<AdminWallet> => {
    const { data } = await api.post(`/api/v1/admin/wallets/user/${userId}/freeze`);
    return data;
  },

  unfreezeWallet: async (userId: string): Promise<AdminWallet> => {
    const { data } = await api.post(`/api/v1/admin/wallets/user/${userId}/unfreeze`);
    return data;
  },

  getWalletMetrics: async (): Promise<WalletMetrics> => {
    const { data } = await api.get('/api/v1/admin/wallets/metrics');
    return data;
  },

  // Audit Logs
  getAuditLogs: async (params: AuditLogFilterParams = {}): Promise<PageResponse<AuditLogEntry>> => {
    const { data } = await api.get('/api/v1/admin/users/audit-logs', { params });
    return data;
  },

  searchAuditLogs: async (params: AuditLogFilterParams = {}): Promise<PageResponse<AuditLogEntry>> => {
    const { data } = await api.get('/api/v1/admin/users/audit-logs/search', { params });
    return data;
  },
};
