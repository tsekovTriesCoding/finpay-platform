import api from './axios';

// Types

export interface StatusTimelineEntry {
  status: string;
  label: string;
  description: string;
  timestamp: string | null;
  completed: boolean;
  current: boolean;
  failed: boolean;
}

export type TransactionType = 'TRANSFER' | 'BILL_PAYMENT' | 'MONEY_REQUEST';

export interface TransactionDetail {
  id: string;
  transactionReference: string;
  type: TransactionType;

  // Parties
  senderUserId: string | null;
  recipientUserId: string | null;

  // Amounts
  amount: number;
  currency: string;
  processingFee: number;
  totalAmount: number;

  // Status
  status: string;
  failureReason: string | null;

  // Descriptive
  title: string;
  subtitle: string;
  description: string | null;

  // Metadata
  metadata: Record<string, unknown>;

  // Timeline
  timeline: StatusTimelineEntry[];

  // Actions
  availableActions: string[];

  // Timestamps
  createdAt: string;
  completedAt: string | null;
  updatedAt: string | null;
}

// API Service

export const transactionDetailService = {
  /** Fetch detailed transfer view including timeline and available actions. */
  getTransferDetail: async (transferId: string): Promise<TransactionDetail> => {
    const response = await api.get<TransactionDetail>(
      `/api/v1/transactions/transfers/${transferId}`,
    );
    return response.data;
  },

  /** Fetch detailed bill payment view including timeline and available actions. */
  getBillPaymentDetail: async (billPaymentId: string): Promise<TransactionDetail> => {
    const response = await api.get<TransactionDetail>(
      `/api/v1/transactions/bills/${billPaymentId}`,
    );
    return response.data;
  },

  /** Fetch detailed money request view including timeline and available actions. */
  getMoneyRequestDetail: async (requestId: string): Promise<TransactionDetail> => {
    const response = await api.get<TransactionDetail>(
      `/api/v1/transactions/requests/${requestId}`,
    );
    return response.data;
  },
};
