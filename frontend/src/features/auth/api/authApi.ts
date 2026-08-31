import { apiClient } from '@/lib/api/client';
import { CurrentUser, LoginRequest, LoginResponse, RefreshTokenRequest } from '@/types/auth';

export const authApi = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const payload = {
      employeeCode: credentials.usernameOrEmail,
      email: credentials.usernameOrEmail,
      password: credentials.password,
    };
    const response = await apiClient.post<LoginResponse>('/auth/login', payload);
    return response.data;
  },

  async refreshToken(refreshToken: string): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse, RefreshTokenRequest>('/auth/refresh', { refreshToken });
    return response.data;
  },

  async getCurrentUser(): Promise<CurrentUser> {
    const response = await apiClient.get<CurrentUser>('/auth/me');
    return response.data;
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      // Ignore network errors on logout
    }
  },

  async changePassword(passwords: { currentPassword: string; newPassword: string }): Promise<void> {
    await apiClient.post('/auth/change-password', passwords);
  },
};
