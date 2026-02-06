import api from './axios';

export interface UserSearchResult {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  profileImageUrl: string | null;
}

export interface UserSearchResponse {
  content: UserSearchResult[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface MoneyTransferRequest {
  recipientUserId: string;
  amount: number;
  currency: string;
  description?: string;
}

export interface MoneyTransfer {
  id: string;
  transactionReference: string;
  senderUserId: string;
  recipientUserId: string;
  amount: number;
  currency: string;
  description: string | null;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'COMPENSATING' | 'COMPENSATED';
  failureReason: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface TransferHistoryResponse {
  content: MoneyTransfer[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const paymentService = {
  /**
   * Search users by name or email for transfer recipient selection.
   */
  searchUsers: async (query: string, excludeUserId: string): Promise<UserSearchResponse> => {
    const response = await api.get<UserSearchResponse>('/api/v1/users/search', {
      params: {
        query,
        excludeUserId,
        page: 0,
        size: 5,
      },
    });
    return response.data;
  },

  /**
   * Initiate a money transfer to another user.
   */
  sendMoney: async (senderUserId: string, request: MoneyTransferRequest): Promise<MoneyTransfer> => {
    const response = await api.post<MoneyTransfer>('/api/v1/payments/transfers', request, {
      headers: {
        'X-User-Id': senderUserId,
      },
    });
    return response.data;
  },

  /**
   * Get transfer by ID.
   */
  getTransfer: async (transferId: string): Promise<MoneyTransfer> => {
    const response = await api.get<MoneyTransfer>(`/api/v1/payments/transfers/${transferId}`);
    return response.data;
  },

  /**
   * Get transfer history for a user.
   */
  getTransferHistory: async (userId: string, page = 0, size = 10): Promise<TransferHistoryResponse> => {
    const response = await api.get<TransferHistoryResponse>(`/api/v1/payments/transfers/user/${userId}`, {
      params: { page, size },
    });
    return response.data;
  },
};

export default paymentService;
