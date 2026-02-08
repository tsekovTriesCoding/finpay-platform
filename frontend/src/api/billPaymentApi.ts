import api from './axios';

// Types

export type BillCategory =
  | 'ELECTRICITY'
  | 'WATER'
  | 'INTERNET'
  | 'PHONE'
  | 'GAS'
  | 'INSURANCE'
  | 'RENT'
  | 'SUBSCRIPTION'
  | 'GOVERNMENT'
  | 'EDUCATION'
  | 'OTHER';

export type BillPaymentStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'REFUNDED'
  | 'COMPENSATING'
  | 'COMPENSATED';

export interface BillPaymentRequest {
  userId: string;
  category: BillCategory;
  billerName: string;
  billerCode: string;
  accountNumber: string;
  accountHolderName?: string;
  amount: number;
  currency?: string;
  description?: string;
}

export interface BillPayment {
  id: string;
  userId: string;
  transactionReference: string;
  category: BillCategory;
  billerName: string;
  billerCode: string;
  accountNumber: string;
  accountHolderName: string | null;
  amount: number;
  currency: string;
  processingFee: number;
  totalAmount: number;
  status: BillPaymentStatus;
  description: string | null;
  failureReason: string | null;
  billerReference: string | null;
  processedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BillPaymentPageResponse {
  content: BillPayment[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// Static biller catalogue (v1)

export interface Biller {
  code: string;
  name: string;
  category: BillCategory;
  icon: string; // Lucide icon name reference
}

export const BILLERS: Biller[] = [
  // Electricity
  { code: 'ELEC-001', name: 'City Power Co.', category: 'ELECTRICITY', icon: 'Zap' },
  { code: 'ELEC-002', name: 'National Grid Energy', category: 'ELECTRICITY', icon: 'Zap' },

  // Water
  { code: 'WATER-001', name: 'Metro Water Supply', category: 'WATER', icon: 'Droplets' },
  { code: 'WATER-002', name: 'AquaPure Utilities', category: 'WATER', icon: 'Droplets' },

  // Internet
  { code: 'NET-001', name: 'FiberNet ISP', category: 'INTERNET', icon: 'Wifi' },
  { code: 'NET-002', name: 'SpeedLink Broadband', category: 'INTERNET', icon: 'Wifi' },

  // Phone
  { code: 'PHONE-001', name: 'TeleCom Mobile', category: 'PHONE', icon: 'Smartphone' },
  { code: 'PHONE-002', name: 'ConnectCell Wireless', category: 'PHONE', icon: 'Smartphone' },

  // Gas
  { code: 'GAS-001', name: 'PipeGas Energy', category: 'GAS', icon: 'Flame' },

  // Insurance
  { code: 'INS-001', name: 'SafeGuard Insurance', category: 'INSURANCE', icon: 'Shield' },
  { code: 'INS-002', name: 'TrustLife Coverage', category: 'INSURANCE', icon: 'Shield' },

  // Rent
  { code: 'RENT-001', name: 'HomeSpace Properties', category: 'RENT', icon: 'Home' },

  // Subscriptions
  { code: 'SUB-001', name: 'StreamMax Plus', category: 'SUBSCRIPTION', icon: 'Tv' },
  { code: 'SUB-002', name: 'CloudStore Premium', category: 'SUBSCRIPTION', icon: 'Cloud' },

  // Government
  { code: 'GOV-001', name: 'Municipal Tax Office', category: 'GOVERNMENT', icon: 'Building2' },

  // Education
  { code: 'EDU-001', name: 'State University Fees', category: 'EDUCATION', icon: 'GraduationCap' },
];

export const BILL_CATEGORY_LABELS: Record<BillCategory, string> = {
  ELECTRICITY: 'Electricity',
  WATER: 'Water',
  INTERNET: 'Internet',
  PHONE: 'Phone',
  GAS: 'Gas',
  INSURANCE: 'Insurance',
  RENT: 'Rent',
  SUBSCRIPTION: 'Subscriptions',
  GOVERNMENT: 'Government',
  EDUCATION: 'Education',
  OTHER: 'Other',
};

// API Service

export const billPaymentService = {
  payBill: async (request: BillPaymentRequest): Promise<BillPayment> => {
    const response = await api.post<BillPayment>('/api/v1/payments/bills', request);
    return response.data;
  },

  getBillPayment: async (id: string): Promise<BillPayment> => {
    const response = await api.get<BillPayment>(`/api/v1/payments/bills/${id}`);
    return response.data;
  },

  getUserBillPayments: async (
    userId: string,
    page = 0,
    size = 10,
  ): Promise<BillPaymentPageResponse> => {
    const response = await api.get<BillPaymentPageResponse>(
      `/api/v1/payments/bills/user/${userId}`,
      { params: { page, size } },
    );
    return response.data;
  },

  cancelBillPayment: async (id: string): Promise<BillPayment> => {
    const response = await api.post<BillPayment>(`/api/v1/payments/bills/${id}/cancel`);
    return response.data;
  },

  getCategories: async (): Promise<BillCategory[]> => {
    const response = await api.get<BillCategory[]>('/api/v1/payments/bills/categories');
    return response.data;
  },
};
