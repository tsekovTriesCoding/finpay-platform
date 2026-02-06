import api, { API_GATEWAY_URL } from './axios';

export interface RegisterData {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
}

export interface LoginData {
  email: string;
  password: string;
}

export interface User {
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
  createdAt: string;
  updatedAt: string;
  lastLoginAt: string | null;
}

export interface AuthResponse {
  accessToken: string | null;
  refreshToken: string | null;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export const authService = {
  register: async (data: RegisterData): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/api/v1/auth/register', data);
    return response.data;
  },

  login: async (data: LoginData): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/api/v1/auth/login', data);
    return response.data;
  },

  refreshToken: async (): Promise<AuthResponse> => {
    // Refresh token is sent automatically via HTTP-only cookie
    const response = await api.post<AuthResponse>('/api/v1/auth/refresh');
    return response.data;
  },

  logout: async (): Promise<void> => {
    // Refresh token is sent automatically via HTTP-only cookie
    await api.post('/api/v1/auth/logout');
  },

  logoutAll: async (): Promise<void> => {
    await api.post('/api/v1/auth/logout-all');
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await api.get<User>('/api/v1/auth/me');
    return response.data;
  },

  // OAuth URLs go through API Gateway which routes to auth-service
  getGoogleLoginUrl: (): string => {
    return `${API_GATEWAY_URL}/oauth2/authorization/google`;
  },

  getGithubLoginUrl: (): string => {
    return `${API_GATEWAY_URL}/oauth2/authorization/github`;
  },
};

export default authService;
