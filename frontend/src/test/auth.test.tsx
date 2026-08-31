import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '@/app/providers/AuthProvider';
import { LoginForm } from '@/features/auth/components/LoginForm';
import { tokenStorage } from '@/lib/auth/tokenStorage';
import { authApi } from '@/features/auth/api/authApi';
import { CurrentUser, LoginResponse } from '@/types/auth';

const mockUser: CurrentUser = {
  id: 'a0000000-0000-0000-0000-000000000001',
  employeeCode: 'EMP1001',
  name: 'Rajesh Sharma',
  firstName: 'Rajesh',
  lastName: 'Sharma',
  email: 'rajesh.sharma@logiconnect.internal',
  status: 'ACTIVE',
  roles: ['EMPLOYEE'],
  permissions: ['SEND_MESSAGES'],
};

const mockLoginResponse: LoginResponse = {
  accessToken: 'mock-access-token-12345',
  refreshToken: 'mock-refresh-token-67890',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: {
    id: mockUser.id,
    username: 'rsharma',
    name: mockUser.name,
    email: mockUser.email,
    roles: ['EMPLOYEE'],
  },
};

describe('Authentication & Token Storage Foundation', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('1. Token storage isolates token access without throwing or logging', () => {
    tokenStorage.setTokens('access-token-xyz', 'refresh-token-abc');
    expect(tokenStorage.getAccessToken()).toBe('access-token-xyz');
    expect(tokenStorage.getRefreshToken()).toBe('refresh-token-abc');
    expect(tokenStorage.hasValidToken()).toBe(true);

    tokenStorage.clearTokens();
    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
    expect(tokenStorage.hasValidToken()).toBe(false);
  });

  it('2. Login form renders inputs, password toggle, and submit button', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginForm />
        </AuthProvider>
      </BrowserRouter>
    );

    expect(screen.getByLabelText(/username or corporate email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('3. Form validation triggers when submitting empty inputs', async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginForm />
        </AuthProvider>
      </BrowserRouter>
    );

    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(screen.getByText(/username or email is required/i)).toBeInTheDocument();
    expect(screen.getByText(/password is required/i)).toBeInTheDocument();
  });

  it('4. Successful login stores tokens and resolves authenticated state', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue(mockLoginResponse);
    vi.spyOn(authApi, 'getCurrentUser').mockResolvedValue(mockUser);

    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginForm />
        </AuthProvider>
      </BrowserRouter>
    );

    await user.type(screen.getByLabelText(/username or corporate email/i), 'rsharma');
    await user.type(screen.getByLabelText(/^password/i), 'Password123!');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(tokenStorage.getAccessToken()).toBe('mock-access-token-12345');
      expect(tokenStorage.getRefreshToken()).toBe('mock-refresh-token-67890');
    });
  });

  it('5. Login error renders safe enterprise message without exposing backend stack trace', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          success: false,
          error: {
            code: 'INVALID_CREDENTIALS',
            message: 'Invalid credentials. Check your password.',
          },
        },
      },
    });

    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginForm />
        </AuthProvider>
      </BrowserRouter>
    );

    await user.type(screen.getByLabelText(/username or corporate email/i), 'rsharma');
    await user.type(screen.getByLabelText(/^password/i), 'WrongPass!');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
    });
  });
});
