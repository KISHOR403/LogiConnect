import { Role } from '@/types/auth';

export const ROLES: Record<string, Role> = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  HR_ADMIN: 'HR_ADMIN',
  MANAGER: 'MANAGER',
  TEAM_LEADER: 'TEAM_LEADER',
  EMPLOYEE: 'EMPLOYEE',
} as const;

export const ADMIN_ROLES: Role[] = ['SUPER_ADMIN', 'HR_ADMIN'];
export const MANAGEMENT_ROLES: Role[] = ['SUPER_ADMIN', 'HR_ADMIN', 'MANAGER', 'TEAM_LEADER'];

export const ROLE_LABELS: Record<Role, string> = {
  SUPER_ADMIN: 'Super Administrator',
  HR_ADMIN: 'HR Administrator',
  MANAGER: 'Department Manager',
  TEAM_LEADER: 'Team Lead',
  EMPLOYEE: 'Employee',
};

export const ROLE_BADGE_VARIANTS: Record<Role, 'brand' | 'success' | 'warning' | 'danger' | 'neutral'> = {
  SUPER_ADMIN: 'danger',
  HR_ADMIN: 'brand',
  MANAGER: 'warning',
  TEAM_LEADER: 'success',
  EMPLOYEE: 'neutral',
};
