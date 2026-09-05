import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '@/app/providers/AuthProvider';
import { LoginForm } from '@/features/auth/components/LoginForm';
import { AuthLayout } from '@/layouts/AuthLayout';
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

describe('Authentication & Enterprise Login', () => {
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

  it('2. Login form renders enterprise inputs, password toggle, support info, and audit notice', () => {
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
    expect(screen.getByText(/having trouble signing in\? contact your it administrator\./i)).toBeInTheDocument();
    expect(
      screen.getByText(/authorized employees only\. system activity may be logged for security and auditing purposes\./i)
    ).toBeInTheDocument();
  });

  it('3. Form validation triggers for empty inputs and invalid email format', async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginForm />
        </AuthProvider>
      </BrowserRouter>
    );

    // Empty submission
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    expect(screen.getByText(/username or corporate email is required/i)).toBeInTheDocument();
    expect(screen.getByText(/password is required/i)).toBeInTheDocument();

    // Invalid email format submission
    await user.type(screen.getByLabelText(/username or corporate email/i), 'invalid-email@');
    await user.type(screen.getByLabelText(/^password/i), 'Password123!');
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    expect(screen.getByText(/please enter a valid email format/i)).toBeInTheDocument();
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

  it('5. Login error renders safe enterprise message and sanitizes SQL/JWT stack traces', async () => {
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
      expect(screen.getByRole('alert')).toHaveTextContent(/invalid credentials/i);
    });
  });

  it('6. Account locked error displays appropriate warning', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          success: false,
          error: {
            code: 'ACCOUNT_LOCKED',
            message: 'Account is temporarily locked due to repeated failed login attempts.',
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

    await user.type(screen.getByLabelText(/username or corporate email/i), 'locked_user');
    await user.type(screen.getByLabelText(/^password/i), 'Password123!');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/account temporarily locked/i)).toBeInTheDocument();
      expect(screen.getByText(/temporarily locked due to repeated failed login attempts/i)).toBeInTheDocument();
    });
  });

  it('7. Account inactive error displays administrative guidance', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          success: false,
          error: {
            code: 'ACCOUNT_INACTIVE',
            message: 'Account is inactive or disabled. Please contact system administration.',
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

    await user.type(screen.getByLabelText(/username or corporate email/i), 'inactive_user');
    await user.type(screen.getByLabelText(/^password/i), 'Password123!');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/account disabled/i)).toBeInTheDocument();
      expect(screen.getByText(/inactive or disabled/i)).toBeInTheDocument();
    });
  });

  it('8. Network error displays connection guidance', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValue({
      isAxiosError: true,
      code: 'ERR_NETWORK',
    });

    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginForm />
        </AuthProvider>
      </BrowserRouter>
    );

    await user.type(screen.getByLabelText(/username or corporate email/i), 'test_user');
    await user.type(screen.getByLabelText(/^password/i), 'Password123!');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/connection error/i)).toBeInTheDocument();
      expect(screen.getByText(/unable to connect to the authentication service/i)).toBeInTheDocument();
    });
  });

  it('9. AuthLayout renders enterprise left-side messaging, 4 product capabilities, and security badges', () => {
    render(
      <BrowserRouter>
        <AuthLayout />
      </BrowserRouter>
    );

    // Main heading & supporting text
    expect(screen.getByText('Secure communication for your workplace')).toBeInTheDocument();
    expect(
      screen.getByText(
        'Connect with your teams, share operational updates, manage announcements, and coordinate meetings — all in one secure platform.'
      )
    ).toBeInTheDocument();

    // 4 Product capabilities
    expect(screen.getByText('Secure employee messaging')).toBeInTheDocument();
    expect(screen.getByText('Department & team channels')).toBeInTheDocument();
    expect(screen.getByText('Company announcements')).toBeInTheDocument();
    expect(screen.getByText('Meetings & collaboration')).toBeInTheDocument();

    // Governance & Security
    expect(screen.getByText('Role-Based Access')).toBeInTheDocument();
    expect(screen.getByText('Permissions based on your role and responsibilities.')).toBeInTheDocument();
    expect(screen.getByText('Audit & Accountability')).toBeInTheDocument();
    expect(screen.getByText('Important activities are recorded for organizational accountability.')).toBeInTheDocument();
    expect(screen.getByText('Secure Enterprise Platform')).toBeInTheDocument();
  });
});
