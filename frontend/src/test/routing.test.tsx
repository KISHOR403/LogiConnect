import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '@/app/providers/AuthProvider';
import { AppRoutes } from '@/app/routes';
import { authApi } from '@/features/auth/api/authApi';
import { tokenStorage } from '@/lib/auth/tokenStorage';
import { CurrentUser } from '@/types/auth';

const employeeUser: CurrentUser = {
  id: 'e0000000-0000-0000-0000-000000000001',
  employeeCode: 'EMP1001',
  name: 'Vikram Singh',
  firstName: 'Vikram',
  lastName: 'Singh',
  email: 'vikram.singh@logiconnect.internal',
  status: 'ACTIVE',
  roles: ['EMPLOYEE'],
  permissions: ['SEND_MESSAGES'],
};

const adminUser: CurrentUser = {
  id: 'a0000000-0000-0000-0000-000000000001',
  employeeCode: 'ADM1001',
  name: 'Anita Desai',
  firstName: 'Anita',
  lastName: 'Desai',
  email: 'anita.desai@logiconnect.internal',
  status: 'ACTIVE',
  roles: ['SUPER_ADMIN'],
  permissions: ['ALL_PERMISSIONS'],
};

describe('Routing, Protection, & Role-Aware Navigation', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('1. Unauthenticated user accessing /app/dashboard is redirected to /login', async () => {
    render(
      <MemoryRouter initialEntries={['/app/dashboard']}>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/sign in to your account/i)).toBeInTheDocument();
    });
  });

  it('2. Authenticated user can access /app/dashboard and sees workspace shell', async () => {
    tokenStorage.setTokens('valid-token-123');
    vi.spyOn(authApi, 'getCurrentUser').mockResolvedValue(employeeUser);

    render(
      <MemoryRouter initialEntries={['/app/dashboard']}>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/welcome back, vikram/i)).toBeInTheDocument();
      expect(screen.getByText(/operations workspace/i)).toBeInTheDocument();
    });
  });

  it('3. Regular EMPLOYEE does not see Administration links in sidebar navigation', async () => {
    tokenStorage.setTokens('valid-token-123');
    vi.spyOn(authApi, 'getCurrentUser').mockResolvedValue(employeeUser);

    render(
      <MemoryRouter initialEntries={['/app/dashboard']}>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/welcome back, vikram/i)).toBeInTheDocument();
    });

    // Main workspace items exist in navigation links
    expect(screen.getByRole('link', { name: /^messages$/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /^channels$/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /^announcements$/i })).toBeInTheDocument();

    // Admin links do not exist
    expect(screen.queryByText('Administration')).not.toBeInTheDocument();
    expect(screen.queryByText('User Management & Accounts')).not.toBeInTheDocument();
  });

  it('4. SUPER_ADMIN user sees Administration navigation section', async () => {
    tokenStorage.setTokens('valid-admin-token');
    vi.spyOn(authApi, 'getCurrentUser').mockResolvedValue(adminUser);

    render(
      <MemoryRouter initialEntries={['/app/dashboard']}>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/welcome back, anita/i)).toBeInTheDocument();
    });

    expect(screen.getByText('Administration')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /users & access/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /employee master/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /^departments$/i })).toBeInTheDocument();
  });

  it('5. Regular EMPLOYEE trying to access /admin/users is redirected to /unauthorized', async () => {
    tokenStorage.setTokens('valid-token-123');
    vi.spyOn(authApi, 'getCurrentUser').mockResolvedValue(employeeUser);

    render(
      <MemoryRouter initialEntries={['/admin/users']}>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/access restricted/i)).toBeInTheDocument();
    });
  });

  it('6. Invalid route displays Page Not Found (404)', async () => {
    render(
      <MemoryRouter initialEntries={['/some-non-existent-path']}>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/page not found/i)).toBeInTheDocument();
    });
  });
});
