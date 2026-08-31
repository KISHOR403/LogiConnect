# LogiConnect — Frontend Architecture & Foundation Guide

## 1. Overview & Technology Stack

LogiConnect is an enterprise internal communication and operations collaboration platform serving approximately 2,000 employees. The frontend application is architected for high security, role awareness, deterministic state flow, and low-latency interaction.

### Core Technologies
* **Framework / Runtime**: React 18 with TypeScript 5
* **Build Tool**: Vite 5
* **Routing**: React Router DOM (v6)
* **Styling**: Tailwind CSS 3 with custom enterprise tokens and utility layering (`clsx` + `tailwind-merge`)
* **HTTP Client**: Axios with centralized request/response interceptors and isolated token injection
* **Icons**: Lucide React
* **Testing**: Vitest with `@testing-library/react` and `@testing-library/jest-dom`

---

## 2. Directory Structure

```
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.js
├── postcss.config.js
├── src/
│   ├── main.tsx                         # StrictMode entrypoint
│   ├── app/
│   │   ├── App.tsx                      # Root component with providers
│   │   ├── routes.tsx                   # Declarative routing and route guards
│   │   └── providers/
│   │       └── AuthProvider.tsx         # Centralized auth context & token lifecycle
│   ├── assets/
│   │   └── styles/
│   │       └── index.css                # Tailwind directives & enterprise utilities
│   ├── types/
│   │   ├── api.ts                       # Standard API response, error, and page contracts
│   │   ├── auth.ts                      # CurrentUser, Role, Login, and Session models
│   │   ├── notification.ts              # 11 notification types and payload models
│   │   ├── navigation.ts                # Sidebar, section, and breadcrumb models
│   │   └── index.ts                     # Barrel export
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts                # Central Axios client with bearer interceptor
│   │   │   └── errors.ts                # Sanitized error formatter
│   │   ├── auth/
│   │   │   ├── tokenStorage.ts          # Isolated token storage (zero logging/UI leak)
│   │   │   └── permissions.ts           # Role checking and permission verification
│   │   ├── constants/
│   │   │   ├── routes.ts                # Typed route constants
│   │   │   └── roles.ts                 # Role definitions & badge variant mapping
│   │   └── utils/
│   │       ├── cn.ts                    # Class name merge utility
│   │       └── formatDate.ts            # Relative time and timestamp formatters
│   ├── components/
│   │   ├── common/                      # Avatar, SearchBar
│   │   ├── feedback/                    # LoadingSpinner, PageLoader, Skeleton, ButtonLoading, ErrorState, EmptyState
│   │   ├── layout/                      # Header, Sidebar, UserMenu, NotificationDropdown
│   │   ├── navigation/                  # NavItem, ProtectedRoute, RoleRoute, RoleGuard
│   │   └── ui/                          # Button, Input, Card, Badge, Modal
│   ├── features/
│   │   ├── auth/                        # LoginForm, authApi, useAuth hook
│   │   └── notifications/               # notificationApi, NotificationPanel
│   ├── layouts/
│   │   ├── AuthLayout.tsx               # Split-screen branded login shell
│   │   └── DashboardLayout.tsx          # Responsive enterprise dashboard shell
│   ├── pages/
│   │   ├── auth/                        # LoginPage
│   │   ├── dashboard/                   # DashboardPage
│   │   ├── messaging/                   # MessagesPage, ConversationDetailPage
│   │   ├── channels/                    # ChannelsPage, ChannelDetailPage
│   │   ├── announcements/               # AnnouncementsPage, AnnouncementDetailPage
│   │   ├── meetings/                    # MeetingsPage, MeetingDetailPage
│   │   ├── employees/                   # EmployeesPage, EmployeeDetailPage
│   │   ├── documents/                   # DocumentsPage
│   │   ├── notifications/               # NotificationsPage
│   │   ├── profile/                     # ProfilePage
│   │   ├── settings/                    # SettingsPage
│   │   ├── admin/                       # Users, Employees, Departments, Teams, Roles, AuditLogs
│   │   └── errors/                      # NotFoundPage, UnauthorizedPage
│   └── test/
│       ├── setup.ts                     # Testing setup & jsdom polyfills
│       ├── auth.test.tsx                # Auth state, validation & token isolation tests
│       ├── routing.test.tsx             # Protected routes, role guards & 404 tests
│       └── components.test.tsx          # Design system component & state tests
```

---

## 3. Token Security & Lifecycle

Security is enforced at multiple architectural layers:

1. **Zero Logging & UI Leakage**: Access and refresh tokens are strictly held in memory and securely managed via `tokenStorage.ts`. Tokens are never logged to `console.*`, printed in error dialogs, or placed in URL parameters.
2. **Automatic Bearer Injection**: The Axios client in `src/lib/api/client.ts` automatically attaches `Authorization: Bearer <accessToken>` to every outgoing HTTP request.
3. **Queue-Safe Token Refresh**: When an API returns a `401 Unauthorized`, the client queues pending requests, invokes the refresh endpoint once, updates the in-memory token, and replays queued requests without disrupting user interactions.
4. **Session Eviction**: If refresh fails or a `401` persists, a custom `auth:unauthorized` event is broadcast across the application, clearing session memory and redirecting the browser to `/login`.

---

## 4. Role-Based Access Control (RBAC)

The frontend reflects the backend RBAC model:

* **Roles**: `SUPER_ADMIN`, `HR_ADMIN`, `MANAGER`, `TEAM_LEADER`, `EMPLOYEE`.
* **Frontend Guards**:
  - `ProtectedRoute`: Verifies that a valid session exists. If unauthenticated, it preserves the target route in navigation state and redirects to `/login`.
  - `RoleRoute`: Restricts route trees (e.g. `/admin/*`) to specific authorized roles (`ADMIN_ROLES`), redirecting unauthorized users to `/unauthorized`.
  - `RoleGuard`: Declarative UI component that conditionally mounts or hides elements based on roles or permissions.
* **Navigation Filtering**: The `Sidebar` automatically inspects current user roles and dynamically includes the Administration section only for `SUPER_ADMIN` and `HR_ADMIN` users.
* **Security Source of Truth**: All frontend checks are UX-level only; the backend Spring Security filter chain validates every REST request with database-backed permission evaluation.

---

## 5. Error Sanitization & Feedback States

API responses from the backend follow a strict envelope:
* Success: `{ success: true, data: ..., message: ... }`
* Error: `{ success: false, error: { code, message, details }, timestamp, path }`

The `src/lib/api/errors.ts` utility translates raw network and server errors into user-friendly messages, preventing technical stack traces, database schema details, or JWT tokens from reaching the UI.

The platform provides standardized feedback states:
* `PageLoader`: Full-screen and inline route loading states.
* `Skeleton` & `CardSkeleton`: Content placeholders during asynchronous fetches.
* `ErrorState`: Clear error banner with retry triggers.
* `EmptyState`: Clean messages for empty inboxes, channels, broadcasts, and feeds.

---

## 6. Verification & Test Suite

All components, authentication flows, route guards, and UI states are verified using Vitest:

* **Test Execution**: `npm run test` (18/18 tests passing)
* **Build Validation**: `npm run build` (Clean TypeScript check and Vite production bundle generation)
