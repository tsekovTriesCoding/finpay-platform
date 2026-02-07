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
  transferType: 'SEND' | 'REQUEST_PAYMENT';
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'COMPENSATING' | 'COMPENSATED';
  sourceRequestId: string | null;
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

// Money Request types

export type MoneyRequestStatus =
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'DECLINED'
  | 'CANCELLED'
  | 'FAILED'
  | 'EXPIRED'
  | 'COMPENSATING'
  | 'COMPENSATED';

export interface MoneyRequestCreatePayload {
  payerUserId: string;
  amount: number;
  currency: string;
  description?: string;
}

export interface MoneyRequest {
  id: string;
  requestReference: string;
  requesterUserId: string;
  payerUserId: string;
  amount: number;
  currency: string;
  description: string | null;
  status: MoneyRequestStatus;
  sagaStatus: string | null;
  failureReason: string | null;
  approvedAt: string | null;
  declinedAt: string | null;
  completedAt: string | null;
  expiresAt: string | null;
  createdAt: string;
}

export interface MoneyRequestPageResponse {
  content: MoneyRequest[];
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

  // Money Request endpoints

  /**
   * Create a money request (authenticated user = requester).
   */
  createMoneyRequest: async (
    requesterUserId: string,
    payload: MoneyRequestCreatePayload,
  ): Promise<MoneyRequest> => {
    const response = await api.post<MoneyRequest>('/api/v1/payments/requests', payload, {
      headers: { 'X-User-Id': requesterUserId },
    });
    return response.data;
  },

  /**
   * Approve a money request (authenticated user = payer).
   */
  approveMoneyRequest: async (payerUserId: string, requestId: string): Promise<MoneyRequest> => {
    const response = await api.post<MoneyRequest>(
      `/api/v1/payments/requests/${requestId}/approve`,
      null,
      { headers: { 'X-User-Id': payerUserId } },
    );
    return response.data;
  },

  /**
   * Decline a money request (authenticated user = payer).
   */
  declineMoneyRequest: async (payerUserId: string, requestId: string): Promise<MoneyRequest> => {
    const response = await api.post<MoneyRequest>(
      `/api/v1/payments/requests/${requestId}/decline`,
      null,
      { headers: { 'X-User-Id': payerUserId } },
    );
    return response.data;
  },

  /**
   * Cancel a money request (authenticated user = requester).
   */
  cancelMoneyRequest: async (requesterUserId: string, requestId: string): Promise<MoneyRequest> => {
    const response = await api.post<MoneyRequest>(
      `/api/v1/payments/requests/${requestId}/cancel`,
      null,
      { headers: { 'X-User-Id': requesterUserId } },
    );
    return response.data;
  },

  /**
   * Get all money requests for a user (as requester or payer).
   */
  getMoneyRequests: async (
    userId: string,
    page = 0,
    size = 10,
  ): Promise<MoneyRequestPageResponse> => {
    const response = await api.get<MoneyRequestPageResponse>(
      `/api/v1/payments/requests/user/${userId}`,
      { params: { page, size } },
    );
    return response.data;
  },

  /**
   * Get pending incoming requests (where user is payer).
   */
  getPendingIncomingRequests: async (
    payerUserId: string,
    page = 0,
    size = 10,
  ): Promise<MoneyRequestPageResponse> => {
    const response = await api.get<MoneyRequestPageResponse>(
      '/api/v1/payments/requests/pending/incoming',
      { headers: { 'X-User-Id': payerUserId }, params: { page, size } },
    );
    return response.data;
  },

  /**
   * Get pending outgoing requests (where user is requester).
   */
  getPendingOutgoingRequests: async (
    requesterUserId: string,
    page = 0,
    size = 10,
  ): Promise<MoneyRequestPageResponse> => {
    const response = await api.get<MoneyRequestPageResponse>(
      '/api/v1/payments/requests/pending/outgoing',
      { headers: { 'X-User-Id': requesterUserId }, params: { page, size } },
    );
    return response.data;
  },

  /**
   * Get count of pending incoming requests (badge count).
   */
  getPendingRequestCount: async (payerUserId: string): Promise<number> => {
    const response = await api.get<number>('/api/v1/payments/requests/pending/count', {
      headers: { 'X-User-Id': payerUserId },
    });
    return response.data;
  },
};

export default paymentService;
