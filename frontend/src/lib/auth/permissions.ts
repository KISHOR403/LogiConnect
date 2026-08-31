import { CurrentUser, Role } from '@/types/auth';
import { ADMIN_ROLES, MANAGEMENT_ROLES } from '@/lib/constants/roles';

export function hasRole(user: CurrentUser | null | undefined, role: Role): boolean {
  if (!user || !user.roles) return false;
  return user.roles.includes(role);
}

export function hasAnyRole(user: CurrentUser | null | undefined, roles: Role[]): boolean {
  if (!user || !user.roles || roles.length === 0) return false;
  return roles.some((r) => user.roles.includes(r));
}

export function hasPermission(user: CurrentUser | null | undefined, permission: string): boolean {
  if (!user || !user.permissions) return false;
  return user.permissions.includes(permission);
}

export function isAdmin(user: CurrentUser | null | undefined): boolean {
  return hasAnyRole(user, ADMIN_ROLES);
}

export function isManagerOrLead(user: CurrentUser | null | undefined): boolean {
  return hasAnyRole(user, MANAGEMENT_ROLES);
}
