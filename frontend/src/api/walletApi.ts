import api from './axios';

export interface Wallet {
  id: string;
  userId: string;
  balance: number;
  reservedBalance: number;
  availableBalance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
}

export const walletService = {
  /**
   * Get or create wallet for a user.
   */
  getWallet: async (userId: string): Promise<Wallet> => {
    const response = await api.get<Wallet>(`/api/v1/wallets/user/${userId}`);
    return response.data;
  },

  /**
   * Get wallet details by user ID (throws if not found).
   */
  getWalletDetails: async (userId: string): Promise<Wallet> => {
    const response = await api.get<Wallet>(`/api/v1/wallets/user/${userId}/details`);
    return response.data;
  },
};

export default walletService;
