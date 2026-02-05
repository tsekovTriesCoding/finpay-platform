import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

// Service-specific base URLs
export const AUTH_API_URL = import.meta.env.VITE_AUTH_API_URL || 'http://localhost:8084';
export const USER_API_URL = import.meta.env.VITE_USER_API_URL || 'http://localhost:8081';
export const PAYMENT_API_URL = import.meta.env.VITE_PAYMENT_API_URL || 'http://localhost:8082';
export const NOTIFICATION_API_URL = import.meta.env.VITE_NOTIFICATION_API_URL || 'http://localhost:8083';

// Create axios instance factory
const createApiInstance = (baseURL: string): AxiosInstance => {
  const instance = axios.create({
    baseURL,
    headers: {
      'Content-Type': 'application/json',
    },
    withCredentials: true,
  });

  // Request interceptor to add auth token
  instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  return instance;
};

// Auth API instance (for auth-service)
export const authApi = createApiInstance(AUTH_API_URL);

// User API instance (for user-service)
export const userApi = createApiInstance(USER_API_URL);

// Payment API instance (for payment-service)
export const paymentApi = createApiInstance(PAYMENT_API_URL);

// Notification API instance (for notification-service)
export const notificationApi = createApiInstance(NOTIFICATION_API_URL);

// Add token refresh interceptor to all service instances
const addRefreshInterceptor = (instance: AxiosInstance) => {
  instance.interceptors.response.use(
    (response) => response,
    async (error) => {
      const originalRequest = error.config;

      // If the error is 401 and we haven't tried to refresh yet
      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;

        try {
          const refreshToken = localStorage.getItem('refreshToken');
          if (refreshToken) {
            const response = await axios.post(`${AUTH_API_URL}/api/v1/auth/refresh`, {
              refreshToken,
            });

            const { accessToken, refreshToken: newRefreshToken } = response.data;
            
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', newRefreshToken);

            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
            return instance(originalRequest);
          }
        } catch (refreshError) {
          // Refresh failed, clear tokens and redirect to login
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          window.location.href = '/login';
          return Promise.reject(refreshError);
        }
      }

      return Promise.reject(error);
    }
  );
};

// Apply refresh interceptor to all API instances
addRefreshInterceptor(authApi);
addRefreshInterceptor(userApi);
addRefreshInterceptor(paymentApi);
addRefreshInterceptor(notificationApi);

// Default export for backwards compatibility
export default authApi;
