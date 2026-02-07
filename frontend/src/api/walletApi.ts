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
   * Get wallet for a user.
   * Wallet is created via Kafka events during registration.
   */
  getWallet: async (userId: string): Promise<Wallet> => {
    const response = await api.get<Wallet>(`/api/v1/wallets/user/${userId}`);
    return response.data;
  },
};

export default walletService;
