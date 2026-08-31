import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthLayout } from '@/layouts/AuthLayout';
import { DashboardLayout } from '@/layouts/DashboardLayout';
import { ProtectedRoute } from '@/components/navigation/ProtectedRoute';
import { RoleRoute } from '@/components/navigation/RoleRoute';
import { ADMIN_ROLES } from '@/lib/constants/roles';
import { ROUTES } from '@/lib/constants/routes';

// Auth Pages
import { LoginPage } from '@/pages/auth/LoginPage';

// Dashboard & Core Workspace Pages
import { DashboardPage } from '@/pages/dashboard/DashboardPage';
import { MessagesPage } from '@/pages/messaging/MessagesPage';
import { ConversationDetailPage } from '@/pages/messaging/ConversationDetailPage';
import { ChannelsPage } from '@/pages/channels/ChannelsPage';
import { ChannelDetailPage } from '@/pages/channels/ChannelDetailPage';
import { AnnouncementsPage } from '@/pages/announcements/AnnouncementsPage';
import { AnnouncementDetailPage } from '@/pages/announcements/AnnouncementDetailPage';
import { MeetingsPage } from '@/pages/meetings/MeetingsPage';
import { MeetingDetailPage } from '@/pages/meetings/MeetingDetailPage';
import { EmployeesPage } from '@/pages/employees/EmployeesPage';
import { EmployeeDetailPage } from '@/pages/employees/EmployeeDetailPage';
import { DocumentsPage } from '@/pages/documents/DocumentsPage';
import { NotificationsPage } from '@/pages/notifications/NotificationsPage';
import { ProfilePage } from '@/pages/profile/ProfilePage';
import { SettingsPage } from '@/pages/settings/SettingsPage';

// Admin Area Pages
import { UsersAdminPage } from '@/pages/admin/UsersAdminPage';
import { EmployeesAdminPage } from '@/pages/admin/EmployeesAdminPage';
import { DepartmentsAdminPage } from '@/pages/admin/DepartmentsAdminPage';
import { TeamsAdminPage } from '@/pages/admin/TeamsAdminPage';
import { RolesAdminPage } from '@/pages/admin/RolesAdminPage';
import { AuditLogsAdminPage } from '@/pages/admin/AuditLogsAdminPage';

// Error Pages
import { NotFoundPage } from '@/pages/errors/NotFoundPage';
import { UnauthorizedPage } from '@/pages/errors/UnauthorizedPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Root redirect */}
      <Route path="/" element={<Navigate to={ROUTES.DASHBOARD} replace />} />

      {/* Public Auth Routes */}
      <Route element={<AuthLayout />}>
        <Route path={ROUTES.LOGIN} element={<LoginPage />} />
      </Route>

      {/* Protected Application Workspace */}
      <Route
        path="/app"
        element={
          <ProtectedRoute>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to={ROUTES.DASHBOARD} replace />} />
        <Route path="dashboard" element={<DashboardPage />} />

        {/* Messaging */}
        <Route path="messages" element={<MessagesPage />} />
        <Route path="messages/:conversationId" element={<ConversationDetailPage />} />

        {/* Channels */}
        <Route path="channels" element={<ChannelsPage />} />
        <Route path="channels/:channelId" element={<ChannelDetailPage />} />

        {/* Announcements */}
        <Route path="announcements" element={<AnnouncementsPage />} />
        <Route path="announcements/:id" element={<AnnouncementDetailPage />} />

        {/* Meetings */}
        <Route path="meetings" element={<MeetingsPage />} />
        <Route path="meetings/:id" element={<MeetingDetailPage />} />

        {/* Employees Directory */}
        <Route path="employees" element={<EmployeesPage />} />
        <Route path="employees/:id" element={<EmployeeDetailPage />} />

        {/* Documents */}
        <Route path="documents" element={<DocumentsPage />} />

        {/* Notifications */}
        <Route path="notifications" element={<NotificationsPage />} />

        {/* User Profile & Settings */}
        <Route path="profile" element={<ProfilePage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>

      {/* Protected Admin Console (Restricted to SUPER_ADMIN & HR_ADMIN) */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute>
            <RoleRoute roles={ADMIN_ROLES}>
              <DashboardLayout />
            </RoleRoute>
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to={ROUTES.ADMIN_USERS} replace />} />
        <Route path="users" element={<UsersAdminPage />} />
        <Route path="employees" element={<EmployeesAdminPage />} />
        <Route path="departments" element={<DepartmentsAdminPage />} />
        <Route path="teams" element={<TeamsAdminPage />} />
        <Route path="roles" element={<RolesAdminPage />} />
        <Route path="audit-logs" element={<AuditLogsAdminPage />} />
      </Route>

      {/* Error & Fallback Routes */}
      <Route path={ROUTES.UNAUTHORIZED} element={<UnauthorizedPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};
