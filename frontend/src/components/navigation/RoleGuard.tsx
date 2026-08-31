import React from 'react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { Role } from '@/types/auth';
import { hasAnyRole, hasPermission, hasRole } from '@/lib/auth/permissions';

export interface RoleGuardProps {
  roles?: Role[];
  role?: Role;
  permission?: string;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export const RoleGuard: React.FC<RoleGuardProps> = ({
  roles,
  role,
  permission,
  children,
  fallback = null,
}) => {
  const { user } = useAuth();

  if (role && !hasRole(user, role)) {
    return <>{fallback}</>;
  }

  if (roles && !hasAnyRole(user, roles)) {
    return <>{fallback}</>;
  }

  if (permission && !hasPermission(user, permission)) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};
