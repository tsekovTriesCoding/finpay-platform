import axios from 'axios';

// API Gateway URL - single entry point for all services
export const API_GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';

// Create the API instance pointing to the API Gateway
const api = axios.create({
  baseURL: API_GATEWAY_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  // Credentials must be true for cookies to be sent/received
  withCredentials: true,
});

// Response interceptor for token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If the error is 401 and we haven't tried to refresh yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Attempt to refresh token via HTTP-only cookie (no token in request body needed)
        await axios.post(`${API_GATEWAY_URL}/api/v1/auth/refresh`, {}, {
          withCredentials: true,
        });

        // Retry the original request - cookies will be automatically sent
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed, clear user data and redirect to login
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

/**
 * Extracts a human-readable error message from an Axios error.
 * Prefers the backend's `message` field (from GlobalExceptionHandler ErrorResponse),
 * falls back to Axios's generic message, then a default string.
 */
export function getApiErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const data = (error as { response?: { data?: { message?: string } } }).response?.data;
    if (data?.message) return data.message;
  }
  if (error instanceof Error) return error.message;
  return fallback;
}

export default api;
