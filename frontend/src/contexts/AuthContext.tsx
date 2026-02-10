import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';

import { authService, type User, type RegisterData, type LoginData, type AuthResponse } from '../api';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (data: LoginData) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  logoutAll: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const setAuthData = useCallback((response: AuthResponse) => {
    localStorage.setItem('user', JSON.stringify(response.user));
    setUser(response.user);
  }, []);

  const clearAuthData = useCallback(() => {
    localStorage.removeItem('user');
    setUser(null);
  }, []);

  // Initialize auth state on mount
  useEffect(() => {
    const initAuth = async () => {
      const storedUser = localStorage.getItem('user');

      if (storedUser) {
        try {
          // /me now returns the full profile (auth-service fetches from user-service internally)
          const currentUser = await authService.getCurrentUser();
          setUser(currentUser);
          localStorage.setItem('user', JSON.stringify(currentUser));
        } catch {
          clearAuthData();
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, [clearAuthData]);

  const login = async (data: LoginData) => {
    const response = await authService.login(data);
    setAuthData(response);
    // Fetch full profile from /me (enriched with user-service data: address, profileImageUrl, etc.)
    try {
      const fullUser = await authService.getCurrentUser();
      setUser(fullUser);
      localStorage.setItem('user', JSON.stringify(fullUser));
    } catch { /* login succeeded, full profile will load on next page refresh */ }
  };

  const register = async (data: RegisterData) => {
    const response = await authService.register(data);
    setAuthData(response);
  };

  const logout = async () => {
    try {
      await authService.logout();
    } finally {
      clearAuthData();
    }
  };

  const logoutAll = async () => {
    try {
      await authService.logoutAll();
    } finally {
      clearAuthData();
    }
  };

  const refreshUser = async () => {
    try {
      // /me now returns the full profile from user-service (address, profileImageUrl, etc.)
      const currentUser = await authService.getCurrentUser();
      setUser(currentUser);
      localStorage.setItem('user', JSON.stringify(currentUser));
    } catch {
      clearAuthData();
    }
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    register,
    logout,
    logoutAll,
    refreshUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
