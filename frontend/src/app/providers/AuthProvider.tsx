import React, { createContext, useCallback, useEffect, useState } from 'react';
import { CurrentUser, LoginRequest, LoginResponse } from '@/types/auth';
import { tokenStorage } from '@/lib/auth/tokenStorage';
import { authApi } from '@/features/auth/api/authApi';

export interface AuthContextType {
  user: CurrentUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<LoginResponse>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(tokenStorage.getAccessToken());
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const fetchCurrentUser = useCallback(async () => {
    try {
      const currentUser = await authApi.getCurrentUser();
      setUser(currentUser);
      return currentUser;
    } catch {
      tokenStorage.clearTokens();
      setAccessToken(null);
      setUser(null);
      return null;
    }
  }, []);

  const refreshAuth = useCallback(async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    if (!refreshToken) {
      tokenStorage.clearTokens();
      setAccessToken(null);
      setUser(null);
      return;
    }

    try {
      const response = await authApi.refreshToken(refreshToken);
      tokenStorage.setTokens(response.accessToken, response.refreshToken);
      setAccessToken(response.accessToken);
      await fetchCurrentUser();
    } catch {
      tokenStorage.clearTokens();
      setAccessToken(null);
      setUser(null);
    }
  }, [fetchCurrentUser]);

  // Initial authentication check on application boot
  useEffect(() => {
    const initAuth = async () => {
      setIsLoading(true);
      const token = tokenStorage.getAccessToken();

      if (token) {
        setAccessToken(token);
        await fetchCurrentUser();
      } else {
        const refreshToken = tokenStorage.getRefreshToken();
        if (refreshToken) {
          await refreshAuth();
        }
      }
      setIsLoading(false);
    };

    initAuth();

    // Listen for custom 401 unauthorized events broadcast by apiClient interceptor
    const handleUnauthorized = () => {
      setAccessToken(null);
      setUser(null);
      tokenStorage.clearTokens();
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [fetchCurrentUser, refreshAuth]);

  const login = useCallback(
    async (credentials: LoginRequest): Promise<LoginResponse> => {
      setIsLoading(true);
      try {
        const response = await authApi.login(credentials);
        tokenStorage.setTokens(response.accessToken, response.refreshToken);
        setAccessToken(response.accessToken);

        // Fetch full profile (department, team, permissions)
        await fetchCurrentUser();
        return response;
      } finally {
        setIsLoading(false);
      }
    },
    [fetchCurrentUser]
  );

  const logout = useCallback(async () => {
    setIsLoading(true);
    try {
      await authApi.logout();
    } finally {
      tokenStorage.clearTokens();
      setAccessToken(null);
      setUser(null);
      setIsLoading(false);
    }
  }, []);

  const value: AuthContextType = {
    user,
    accessToken,
    isAuthenticated: Boolean(accessToken && user),
    isLoading,
    login,
    logout,
    refreshAuth,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
