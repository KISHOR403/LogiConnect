export const ROUTES = {
  // Auth
  LOGIN: '/login',

  // App Base
  APP: '/app',
  DASHBOARD: '/app/dashboard',

  // Core Features
  MESSAGES: '/app/messages',
  MESSAGE_DETAIL: (id: string) => `/app/messages/${id}`,
  CHANNELS: '/app/channels',
  CHANNEL_DETAIL: (id: string) => `/app/channels/${id}`,
  ANNOUNCEMENTS: '/app/announcements',
  ANNOUNCEMENT_DETAIL: (id: string) => `/app/announcements/${id}`,
  MEETINGS: '/app/meetings',
  MEETING_DETAIL: (id: string) => `/app/meetings/${id}`,
  EMPLOYEES: '/app/employees',
  EMPLOYEE_DETAIL: (id: string) => `/app/employees/${id}`,
  DOCUMENTS: '/app/documents',
  NOTIFICATIONS: '/app/notifications',
  PROFILE: '/app/profile',
  SETTINGS: '/app/settings',

  // Admin Area
  ADMIN: '/admin',
  ADMIN_USERS: '/admin/users',
  ADMIN_EMPLOYEES: '/admin/employees',
  ADMIN_DEPARTMENTS: '/admin/departments',
  ADMIN_TEAMS: '/admin/teams',
  ADMIN_ROLES: '/admin/roles',
  ADMIN_AUDIT_LOGS: '/admin/audit-logs',

  // Errors
  UNAUTHORIZED: '/unauthorized',
} as const;
