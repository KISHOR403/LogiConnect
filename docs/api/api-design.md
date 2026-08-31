# LogiConnect REST API Architecture & Design Specification

## 1. Executive Overview & Architecture Principles

**LogiConnect** is an enterprise-grade internal communication and collaboration platform engineered for a distributed logistics workforce of 2,000+ employees across corporate headquarters, regional transport hubs, fulfillment sorting centers, and dispatch lines.

This specification documents the complete REST API contract (`/api/v1`) designed for high throughput, sub-100ms response times, non-repudiation tracking for corporate announcements, and strict enterprise security boundaries.

### Core Architectural Directives

1. **Protocol & Versioning**: All endpoints are prefixed with `/api/v1`. Semantic versioning is strictly enforced.
2. **Resource-Centric REST**: Endpoints represent nouns (e.g. `/api/v1/conversations`, `/api/v1/announcements`). Actions that represent state transitions use idiomatic sub-resource verbs (e.g. `POST /api/v1/announcements/{id}/publish`).
3. **Stateless Authentication**: Standard JSON Web Tokens (JWT) using short-lived Access Tokens (15 minutes) paired with cryptographically secure, rotating Refresh Tokens (7 days) stored securely in `HttpOnly; Secure; SameSite=Strict` cookies or client-side secure store.
4. **Standardized Response Envelope**: All API responses wrap payloads in a predictable envelope structure (`StandardResponse<T>`).
5. **Differentiated Pagination**:
   - **Offset Pagination** (`page`, `size` default 20, max 100) for standard directory and organizational list resources (`employees`, `departments`, `teams`, `documents`, `audit-logs`).
   - **Cursor-Based Pagination** (`cursor`, `limit`, `direction`) for high-volume time-series collections (`messages`, `notifications`, `activity feeds`) to prevent page-drift anomalies and database query degradation under high write concurrency.
6. **Object Storage Decoupling**: Direct binary/multipart upload of large files to application servers is prohibited. The API utilizes a secure Pre-Signed URL upload and completion handshake protocol against S3/MinIO/GCS object stores.
7. **Fine-Grained RBAC & Dynamic Resource Authorization**: Authorization enforces both static permissions (`VIEW_AUDIT_LOGS`, `MANAGE_EMPLOYEES`) and runtime organizational context (e.g., verifying that a user belongs to the department or team whose channel/announcement they are accessing).
8. **Immutability & Non-Repudiation**: Critical audit logs and announcement acknowledgement records are append-only.

---

## 2. Standard Response & Error Envelope Protocol

### 2.1 Standard Success Envelope (`StandardResponse<T>`)

```json
{
  "success": true,
  "data": {
    "id": "e0000000-0000-0000-0000-000000000001",
    "employeeCode": "EMP-1001",
    "firstName": "System",
    "lastName": "Admin",
    "email": "admin@logiconnect.internal"
  },
  "message": "Operation successful"
}
```

### 2.2 Standard Paginated Envelope (`PageResponse<T>`)

```json
{
  "success": true,
  "data": {
    "items": [ /* array of DTOs */ ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 2048,
      "totalPages": 103,
      "first": true,
      "last": false,
      "hasNext": true,
      "hasPrevious": false
    }
  },
  "message": "Directory records retrieved successfully"
}
```

### 2.3 Standard Cursor Paginated Envelope (`CursorResponse<T>`)

```json
{
  "success": true,
  "data": {
    "items": [ /* array of DTOs */ ],
    "cursor": {
      "nextCursor": "MjAyNi0wOC0zMVQxMjowMDowMFpfMDAwMDAwMDE=",
      "prevCursor": "MjAyNi0wOC0zMVQxMTo1OTowMFpfMDAwMDAwMDI=",
      "hasMore": true,
      "limit": 50
    }
  },
  "message": "Message stream retrieved successfully"
}
```

### 2.4 Standard Error Envelope (`ErrorResponse`)

```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Employee with identifier 'e0000000-0000-0000-0000-000000000999' was not found",
    "details": [
      {
        "field": "employeeId",
        "rejectedValue": "e0000000-0000-0000-0000-000000000999",
        "issue": "Entity does not exist in corporate directory"
      }
    ]
  },
  "timestamp": "2026-08-31T12:15:00Z",
  "path": "/api/v1/employees/e0000000-0000-0000-0000-000000000999"
}
```

### 2.5 Standard Error Code Taxonomy

| Error Code | HTTP Status | Meaning |
| :--- | :--- | :--- |
| `BAD_REQUEST` | 400 | Malformed JSON, unparseable parameters, invalid syntax |
| `VALIDATION_FAILED` | 422 | Semantic payload validation errors (e.g. invalid email format, missing required fields) |
| `UNAUTHENTICATED` | 401 | Missing, expired, or cryptographically invalid JWT token |
| `ACCOUNT_LOCKED` | 401 | Account locked due to excessive failed login attempts |
| `UNAUTHORIZED` | 403 | Authenticated user lacks required permission or departmental scope |
| `RESOURCE_NOT_FOUND` | 404 | Target entity UUID does not exist |
| `RESOURCE_CONFLICT` | 409 | Duplicate unique key constraint violation (e.g., employee code, email, slug) |
| `CHANNEL_ACCESS_DENIED` | 403 | Employee attempting to access a channel outside their department/team or private group |
| `READ_ONLY_CHANNEL` | 403 | Non-admin attempting to post to an announcement/read-only channel |
| `PRECONDITION_FAILED` | 412 | Invalid state transition (e.g., publishing an already archived announcement) |
| `RATE_LIMIT_EXCEEDED` | 429 | Exceeded IP or user API rate limits |
| `INTERNAL_SERVER_ERROR` | 500 | Unhandled server exception (audited and sanitized) |

---

## 3. Comprehensive Domain API Specifications

---

### Domain 1: Authentication API (`/api/v1/auth`)

Manages user authentication sessions, JWT issuance, token rotation, and password lifecycle.

#### 1.1 POST `/api/v1/auth/login`
- **Purpose**: Authenticate employee credentials and issue access + refresh tokens.
- **Authentication**: None (Public)
- **Authorization**: Public
- **Request Body**: `LoginRequest` (email, password)
- **Response**: `200 OK` with `AuthTokenResponse` (accessToken, refreshToken, tokenType, expiresIn, userProfile)
- **Error Codes**: `400 BAD_REQUEST`, `401 UNAUTHENTICATED` (Invalid credentials), `401 ACCOUNT_LOCKED`

#### 1.2 POST `/api/v1/auth/refresh`
- **Purpose**: Exchange a valid refresh token for a newly minted access token and rotated refresh token.
- **Authentication**: None (Requires valid refresh token in payload or cookie)
- **Authorization**: Public
- **Request Body**: `RefreshTokenRequest` (refreshToken)
- **Response**: `200 OK` with `AuthTokenResponse`
- **Error Codes**: `401 UNAUTHENTICATED` (Expired or revoked refresh token)

#### 1.3 POST `/api/v1/auth/logout`
- **Purpose**: Invalidate current refresh token session and revoke active authorization.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Request Body**: `LogoutRequest` (refreshToken [optional if cookie-based])
- **Response**: `200 OK` with `StandardResponse<Void>`
- **Error Codes**: `401 UNAUTHENTICATED`

#### 1.4 POST `/api/v1/auth/forgot-password`
- **Purpose**: Trigger a secure password reset email with a cryptographically signed token.
- **Authentication**: None (Public)
- **Authorization**: Public
- **Request Body**: `ForgotPasswordRequest` (email)
- **Response**: `200 OK` with generic success message (to prevent user enumeration)
- **Error Codes**: `429 RATE_LIMIT_EXCEEDED`

#### 1.5 POST `/api/v1/auth/reset-password`
- **Purpose**: Reset user password using the verified reset token.
- **Authentication**: None (Public)
- **Authorization**: Public
- **Request Body**: `ResetPasswordRequest` (token, newPassword)
- **Response**: `200 OK` with `StandardResponse<Void>`
- **Error Codes**: `400 BAD_REQUEST`, `422 VALIDATION_FAILED` (Password complexity)

#### 1.6 POST `/api/v1/auth/change-password`
- **Purpose**: Allow an authenticated user to change their password by supplying current credentials.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Request Body**: `ChangePasswordRequest` (currentPassword, newPassword)
- **Response**: `200 OK` with `StandardResponse<Void>`
- **Error Codes**: `400 BAD_REQUEST`, `401 UNAUTHENTICATED`, `422 VALIDATION_FAILED`

#### 1.7 GET `/api/v1/auth/me`
- **Purpose**: Retrieve currently authenticated principal's user record, associated employee profile, active roles, and granular permission set.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Response**: `200 OK` with `CurrentUserResponse`
- **Error Codes**: `401 UNAUTHENTICATED`

---

### Domain 2: Users API (`/api/v1/users`)

Manages user account security states, password lockouts, and administrative role assignments.

#### 2.1 GET `/api/v1/users`
- **Purpose**: List system user accounts with status and security flags.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_ROLES` or `SUPER_ADMIN`
- **Query Parameters**: `page`, `size`, `status` (`ACTIVE`, `INACTIVE`, `LOCKED`, `PENDING_VERIFICATION`), `search`
- **Response**: `200 OK` with `PageResponse<UserSummaryResponse>`

#### 2.2 GET `/api/v1/users/{id}`
- **Purpose**: Retrieve detailed user security profile and role assignments.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_ROLES` or Self (`id == current_user.id`)
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `UserDetailResponse`
- **Error Codes**: `404 RESOURCE_NOT_FOUND`, `403 UNAUTHORIZED`

#### 2.3 PATCH `/api/v1/users/{id}/status`
- **Purpose**: Administratively activate, deactivate, or unlock a user account.
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN` or `MANAGE_ROLES`
- **Path Parameters**: `id` (UUID)
- **Request Body**: `UpdateUserStatusRequest` (status, reason)
- **Response**: `200 OK` with `UserDetailResponse`
- **Error Codes**: `400 BAD_REQUEST`, `404 RESOURCE_NOT_FOUND`, `403 UNAUTHORIZED`

#### 2.4 GET `/api/v1/users/{id}/roles`
- **Purpose**: List all roles assigned to a user account.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_ROLES` or `SUPER_ADMIN`
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `StandardResponse<List<RoleSummaryResponse>>`

#### 2.5 PUT `/api/v1/users/{id}/roles`
- **Purpose**: Replace role assignments for a user account.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_ROLES` or `SUPER_ADMIN`
- **Path Parameters**: `id` (UUID)
- **Request Body**: `AssignUserRolesRequest` (roleIds)
- **Response**: `200 OK` with `StandardResponse<List<RoleSummaryResponse>>`

---

### Domain 3: Employees API (`/api/v1/employees`)

Corporate HR employee directory, organizational profiles, onboarding, and lifecycle status management.

#### 3.1 GET `/api/v1/employees`
- **Purpose**: Search and filter company directory with pagination.
- **Authentication**: Bearer Token
- **Authorization**: `VIEW_EMPLOYEES` (All active employees)
- **Query Parameters**:
  - `page` (int, default: 0)
  - `size` (int, default: 20, max: 100)
  - `search` (string: matches employeeCode, firstName, lastName, email, designation)
  - `departmentId` (UUID)
  - `teamId` (UUID)
  - `roleId` (UUID)
  - `location` (string: e.g., "Bangalore HQ", "Delhi Fulfillment Center")
  - `status` (string: `ACTIVE`, `PROBATION`, `ON_LEAVE`, `SUSPENDED`, `TERMINATED`, `RESIGNED`)
  - `sort` (string: e.g. `lastName,asc` or `joiningDate,desc`)
- **Response**: `200 OK` with `PageResponse<EmployeeSummaryResponse>`

#### 3.2 GET `/api/v1/employees/{employeeId}`
- **Purpose**: Retrieve detailed profile of an employee.
- **Authentication**: Bearer Token
- **Authorization**: `VIEW_EMPLOYEES`
- **Path Parameters**: `employeeId` (UUID)
- **Response**: `200 OK` with `EmployeeDetailResponse`
- **Error Codes**: `404 RESOURCE_NOT_FOUND`

#### 3.3 POST `/api/v1/employees`
- **Purpose**: Onboard and register a new employee in the company directory.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_EMPLOYEES` (HR_ADMIN, SUPER_ADMIN)
- **Request Body**: `CreateEmployeeRequest` (employeeCode, firstName, lastName, email, phone, designation, departmentId, teamId, managerId, location, joiningDate)
- **Response**: `201 CREATED` with `EmployeeDetailResponse`
- **Error Codes**: `409 RESOURCE_CONFLICT` (Duplicate email or employeeCode), `422 VALIDATION_FAILED`

#### 3.4 PATCH `/api/v1/employees/{employeeId}`
- **Purpose**: Update employee directory profile attributes (contact, designation, team, manager).
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_EMPLOYEES` or Self (`employeeId == current_user.employeeId` for phone/profilePhoto only)
- **Path Parameters**: `employeeId` (UUID)
- **Request Body**: `UpdateEmployeeRequest`
- **Response**: `200 OK` with `EmployeeDetailResponse`
- **Error Codes**: `404 RESOURCE_NOT_FOUND`, `403 UNAUTHORIZED`, `422 VALIDATION_FAILED`

#### 3.5 PATCH `/api/v1/employees/{employeeId}/status`
- **Purpose**: Transition employee employment status (e.g. offboard, place on leave, suspend).
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_EMPLOYEES`
- **Path Parameters**: `employeeId` (UUID)
- **Request Body**: `UpdateEmployeeStatusRequest` (status, exitDate, reason)
- **Response**: `200 OK` with `EmployeeDetailResponse`
- **Error Codes**: `400 BAD_REQUEST`, `404 RESOURCE_NOT_FOUND`

---

### Domain 4: Departments API (`/api/v1/departments`)

Enterprise divisions and department-level hierarchies.

#### 4.1 GET `/api/v1/departments`
- **Purpose**: List all departments with manager details and operational status.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Query Parameters**: `page`, `size`, `status`, `search`
- **Response**: `200 OK` with `PageResponse<DepartmentSummaryResponse>`

#### 4.2 GET `/api/v1/departments/{id}`
- **Purpose**: Retrieve department details including assigned teams and manager summary.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `DepartmentDetailResponse`
- **Error Codes**: `404 RESOURCE_NOT_FOUND`

#### 4.3 POST `/api/v1/departments`
- **Purpose**: Create a new organizational department.
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN` or `HR_ADMIN`
- **Request Body**: `CreateDepartmentRequest` (code, name, description, managerId)
- **Response**: `201 CREATED` with `DepartmentDetailResponse`
- **Error Codes**: `409 RESOURCE_CONFLICT` (Unique code or name violation)

#### 4.4 PATCH `/api/v1/departments/{id}`
- **Purpose**: Update department name, description, or manager.
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN` or `HR_ADMIN`
- **Path Parameters**: `id` (UUID)
- **Request Body**: `UpdateDepartmentRequest`
- **Response**: `200 OK` with `DepartmentDetailResponse`

#### 4.5 GET `/api/v1/departments/{id}/employees`
- **Purpose**: List all employees assigned to this department.
- **Authentication**: Bearer Token
- **Authorization**: `VIEW_EMPLOYEES`
- **Path Parameters**: `id` (UUID)
- **Query Parameters**: `page`, `size`, `status`
- **Response**: `200 OK` with `PageResponse<EmployeeSummaryResponse>`

---

### Domain 5: Teams API (`/api/v1/teams`)

Functional units, operational shifts, and logistics hub teams.

#### 5.1 GET `/api/v1/teams`
- **Purpose**: List teams, optionally filtered by parent department.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Query Parameters**: `page`, `size`, `departmentId`, `status`, `search`
- **Response**: `200 OK` with `PageResponse<TeamSummaryResponse>`

#### 5.2 GET `/api/v1/teams/{id}`
- **Purpose**: Retrieve team details, team lead profile, and department context.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `TeamDetailResponse`
- **Error Codes**: `404 RESOURCE_NOT_FOUND`

#### 5.3 POST `/api/v1/teams`
- **Purpose**: Create a new operational team within a department.
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN`, `HR_ADMIN`, or `MANAGER` (for own department)
- **Request Body**: `CreateTeamRequest` (departmentId, code, name, description, teamLeadId)
- **Response**: `201 CREATED` with `TeamDetailResponse`
- **Error Codes**: `409 RESOURCE_CONFLICT`

#### 5.4 PATCH `/api/v1/teams/{id}`
- **Purpose**: Update team attributes or assign new team lead.
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN`, `HR_ADMIN`, or `MANAGER` (for own department)
- **Path Parameters**: `id` (UUID)
- **Request Body**: `UpdateTeamRequest`
- **Response**: `200 OK` with `TeamDetailResponse`

#### 5.5 GET `/api/v1/teams/{id}/employees`
- **Purpose**: List all employees belonging to this team.
- **Authentication**: Bearer Token
- **Authorization**: `VIEW_EMPLOYEES`
- **Path Parameters**: `id` (UUID)
- **Query Parameters**: `page`, `size`, `status`
- **Response**: `200 OK` with `PageResponse<EmployeeSummaryResponse>`

---

### Domain 6: Roles & Permissions API (`/api/v1/roles`, `/api/v1/permissions`)

RBAC role management and granular permission discovery.

#### 6.1 GET `/api/v1/roles`
- **Purpose**: List all system and custom enterprise roles.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_ROLES` or `SUPER_ADMIN`
- **Response**: `200 OK` with `StandardResponse<List<RoleSummaryResponse>>`

#### 6.2 GET `/api/v1/roles/{id}`
- **Purpose**: Retrieve role details and full associated permission matrix.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_ROLES` or `SUPER_ADMIN`
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `RoleDetailResponse`
- **Error Codes**: `404 RESOURCE_NOT_FOUND`

#### 6.3 POST `/api/v1/roles`
- **Purpose**: Create a new custom security role.
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN`
- **Request Body**: `CreateRoleRequest` (name, displayName, description, permissionIds)
- **Response**: `201 CREATED` with `RoleDetailResponse`
- **Error Codes**: `409 RESOURCE_CONFLICT`

#### 6.4 PATCH `/api/v1/roles/{id}`
- **Purpose**: Update custom role metadata (system roles cannot be renamed).
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN`
- **Path Parameters**: `id` (UUID)
- **Request Body**: `UpdateRoleRequest`
- **Response**: `200 OK` with `RoleDetailResponse`
- **Error Codes**: `400 BAD_REQUEST` (Attempt to modify system role)

#### 6.5 DELETE `/api/v1/roles/{id}`
- **Purpose**: Delete a custom role (prohibited for `is_system = true` roles).
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN`
- **Path Parameters**: `id` (UUID)
- **Response**: `204 NO CONTENT`
- **Error Codes**: `400 BAD_REQUEST` (System role deletion prohibited), `409 RESOURCE_CONFLICT` (Role currently assigned to users)

#### 6.6 PUT `/api/v1/roles/{id}/permissions`
- **Purpose**: Synchronize granular permissions assigned to a role.
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN`
- **Path Parameters**: `id` (UUID)
- **Request Body**: `AssignRolePermissionsRequest` (permissionIds)
- **Response**: `200 OK` with `RoleDetailResponse`

#### 6.7 GET `/api/v1/permissions`
- **Purpose**: List all system permissions grouped by module (`EMPLOYEE`, `ANNOUNCEMENT`, `CHANNEL`, `CONVERSATION`, `DOCUMENT`, `MEETING`, `AUDIT`, `SYSTEM`).
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_ROLES` or `SUPER_ADMIN`
- **Response**: `200 OK` with `StandardResponse<List<PermissionGroupResponse>>`

---

### Domain 7: Conversations API (`/api/v1/conversations`)

Direct 1-to-1 chats and ad-hoc multi-party group messaging rooms.

#### 7.1 GET `/api/v1/conversations`
- **Purpose**: List all direct and group conversations for the current user, ordered by `last_message_at DESC`.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Query Parameters**: `page`, `size`, `type` (`DIRECT`, `GROUP`), `isArchived`
- **Response**: `200 OK` with `PageResponse<ConversationSummaryResponse>`

#### 7.2 GET `/api/v1/conversations/{id}`
- **Purpose**: Retrieve conversation details, active member list, and unread metrics.
- **Authentication**: Bearer Token
- **Authorization**: Member of Conversation
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `ConversationDetailResponse`
- **Error Codes**: `403 UNAUTHORIZED` (Non-member access rejected), `404 RESOURCE_NOT_FOUND`

#### 7.3 POST `/api/v1/conversations/direct`
- **Purpose**: Initiate or retrieve an existing 1-to-1 direct conversation with another employee.
- **Idempotency & Deduplication**: If a direct conversation already exists between `current_user` and `targetUserId`, returns existing conversation with status `200 OK` instead of creating duplicate records.
- **Authentication**: Bearer Token
- **Authorization**: `SEND_MESSAGE`
- **Request Body**: `CreateDirectConversationRequest` (targetUserId)
- **Response**: `201 CREATED` (if new) or `200 OK` (if existing) with `ConversationDetailResponse`
- **Error Codes**: `400 BAD_REQUEST` (Targeting self), `404 RESOURCE_NOT_FOUND` (Target user inactive/missing)

#### 7.4 POST `/api/v1/conversations/group`
- **Purpose**: Create an ad-hoc multi-member group conversation.
- **Authentication**: Bearer Token
- **Authorization**: `SEND_MESSAGE`
- **Request Body**: `CreateGroupConversationRequest` (name, description, memberUserIds, avatarUrl)
- **Response**: `201 CREATED` with `ConversationDetailResponse`

#### 7.5 GET `/api/v1/conversations/{id}/members`
- **Purpose**: List participants belonging to this conversation.
- **Authentication**: Bearer Token
- **Authorization**: Member of Conversation
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `StandardResponse<List<ConversationMemberResponse>>`

#### 7.6 POST `/api/v1/conversations/{id}/members`
- **Purpose**: Add new members to a group conversation.
- **Authentication**: Bearer Token
- **Authorization**: Group Admin or Member (based on conversation configuration)
- **Path Parameters**: `id` (UUID)
- **Request Body**: `AddConversationMembersRequest` (userIds)
- **Response**: `200 OK` with `StandardResponse<List<ConversationMemberResponse>>`
- **Error Codes**: `400 BAD_REQUEST` (Direct conversations cannot accept additional members)

#### 7.7 DELETE `/api/v1/conversations/{id}/members/{userId}`
- **Purpose**: Remove a member from a group chat or leave the conversation.
- **Authentication**: Bearer Token
- **Authorization**: Group Admin OR Self (`userId == current_user.id`)
- **Path Parameters**: `id` (UUID), `userId` (UUID)
- **Response**: `204 NO CONTENT`

---

### Domain 8: Messages API (`/api/v1/conversations/{id}/messages`, `/api/v1/channels/{id}/messages`, `/api/v1/messages/{id}`)

Unified, high-throughput message streaming with cursor-based pagination and rich attachment support.

#### 8.1 GET `/api/v1/conversations/{id}/messages`
- **Purpose**: Fetch chronological message history for a conversation using cursor pagination.
- **Authentication**: Bearer Token
- **Authorization**: Member of Conversation
- **Path Parameters**: `id` (UUID)
- **Query Parameters**:
  - `cursor` (Base64-encoded string representing `created_at` and `message_id`)
  - `limit` (int, default: 50, max: 100)
  - `direction` (string: `BEFORE` [default, loads older messages], `AFTER` [loads newer messages])
- **Response**: `200 OK` with `CursorResponse<MessageResponse>`
- **Error Codes**: `403 UNAUTHORIZED`, `404 RESOURCE_NOT_FOUND`

#### 8.2 POST `/api/v1/conversations/{id}/messages`
- **Purpose**: Send a message to a conversation.
- **Authentication**: Bearer Token
- **Authorization**: Member of Conversation with `SEND_MESSAGE`
- **Path Parameters**: `id` (UUID)
- **Request Body**: `SendMessageRequest` (messageType: `TEXT`, `FILE`, `IMAGE`, `LOCATION`, `AUDIO`, `VIDEO`; content; replyToMessageId; attachments: `[{storageKey, fileUrl, fileName, fileType, fileSize}]`)
- **Response**: `201 CREATED` with `MessageResponse`

#### 8.3 GET `/api/v1/channels/{id}/messages`
- **Purpose**: Fetch chronological message stream for an authorized channel using cursor pagination.
- **Authentication**: Bearer Token
- **Authorization**: Authorized Channel Member/Employee
- **Path Parameters**: `id` (UUID)
- **Query Parameters**: `cursor`, `limit`, `direction`
- **Response**: `200 OK` with `CursorResponse<MessageResponse>`

#### 8.4 POST `/api/v1/channels/{id}/messages`
- **Purpose**: Post a message to an organizational channel.
- **Authentication**: Bearer Token
- **Authorization**: Authorized Channel Member with `SEND_MESSAGE`. For `is_read_only = true` channels, requires `MANAGE_CHANNEL` or `SUPER_ADMIN`.
- **Path Parameters**: `id` (UUID)
- **Request Body**: `SendMessageRequest`
- **Response**: `201 CREATED` with `MessageResponse`
- **Error Codes**: `403 READ_ONLY_CHANNEL`

#### 8.5 PATCH `/api/v1/messages/{id}`
- **Purpose**: Edit message content (updates `edited_at` timestamp).
- **Authentication**: Bearer Token
- **Authorization**: Message Sender (`sender_id == current_user.id`)
- **Path Parameters**: `id` (UUID)
- **Request Body**: `EditMessageRequest` (content)
- **Response**: `200 OK` with `MessageResponse`
- **Error Codes**: `403 UNAUTHORIZED`, `404 RESOURCE_NOT_FOUND`

#### 8.6 DELETE `/api/v1/messages/{id}`
- **Purpose**: Soft-delete a message (sets `deleted_at`, masks content in response).
- **Authentication**: Bearer Token
- **Authorization**: Message Sender OR Channel Admin/Moderator (`DELETE_MESSAGE`)
- **Path Parameters**: `id` (UUID)
- **Response**: `204 NO CONTENT`

---

### Domain 9: Channels API (`/api/v1/channels`)

Workspaces aligned with organizational hierarchy (Company, Department, Team, Private).

#### 9.1 GET `/api/v1/channels`
- **Purpose**: List all channels accessible to current employee based on company, department, and team affiliations + joined private channels.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Query Parameters**: `page`, `size`, `type` (`COMPANY`, `DEPARTMENT`, `TEAM`, `PRIVATE`), `departmentId`, `teamId`, `search`
- **Response**: `200 OK` with `PageResponse<ChannelSummaryResponse>`

#### 9.2 GET `/api/v1/channels/{id}`
- **Purpose**: Retrieve channel metadata, topic, rules, and participant counts.
- **Authentication**: Bearer Token
- **Authorization**: Channel Access Authorization:
  - `COMPANY`: Accessible to all active employees.
  - `DEPARTMENT`: Requires `current_user.employee.department_id == channel.department_id` or `SUPER_ADMIN`.
  - `TEAM`: Requires `current_user.employee.team_id == channel.team_id` or `SUPER_ADMIN`.
  - `PRIVATE`: Requires explicit record in `channel_members`.
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `ChannelDetailResponse`
- **Error Codes**: `403 CHANNEL_ACCESS_DENIED`, `404 RESOURCE_NOT_FOUND`

#### 9.3 POST `/api/v1/channels`
- **Purpose**: Create a new organizational channel.
- **Authentication**: Bearer Token
- **Authorization**: `CREATE_CHANNEL` (SUPER_ADMIN, MANAGER, TEAM_LEADER for respective scopes)
- **Request Body**: `CreateChannelRequest` (name, slug, description, type, departmentId, teamId, isReadOnly)
- **Response**: `201 CREATED` with `ChannelDetailResponse`
- **Error Codes**: `409 RESOURCE_CONFLICT` (Duplicate slug)

#### 9.4 PATCH `/api/v1/channels/{id}`
- **Purpose**: Update channel settings, description, or read-only status.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_CHANNEL` or Channel Admin
- **Path Parameters**: `id` (UUID)
- **Request Body**: `UpdateChannelRequest`
- **Response**: `200 OK` with `ChannelDetailResponse`

#### 9.5 DELETE `/api/v1/channels/{id}`
- **Purpose**: Archive or soft-delete a channel.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_CHANNEL` or `SUPER_ADMIN`
- **Path Parameters**: `id` (UUID)
- **Response**: `204 NO CONTENT`

#### 9.6 GET `/api/v1/channels/{id}/members`
- **Purpose**: List explicit members for a channel.
- **Authentication**: Bearer Token
- **Authorization**: Authorized Channel Member
- **Path Parameters**: `id` (UUID)
- **Query Parameters**: `page`, `size`
- **Response**: `200 OK` with `PageResponse<ChannelMemberResponse>`

#### 9.7 POST `/api/v1/channels/{id}/members`
- **Purpose**: Add member to a private channel or customize notification preferences.
- **Authentication**: Bearer Token
- **Authorization**: Channel Admin/Moderator (or self-join for public channels)
- **Path Parameters**: `id` (UUID)
- **Request Body**: `AddChannelMembersRequest` (userIds, role: `MEMBER`, `MODERATOR`)
- **Response**: `200 OK` with `StandardResponse<List<ChannelMemberResponse>>`

#### 9.8 DELETE `/api/v1/channels/{id}/members/{userId}`
- **Purpose**: Remove member from channel or leave channel.
- **Authentication**: Bearer Token
- **Authorization**: Channel Admin OR Self (`userId == current_user.id`)
- **Path Parameters**: `id` (UUID), `userId` (UUID)
- **Response**: `204 NO CONTENT`

---

### Domain 10: Announcements API (`/api/v1/announcements`)

Corporate broadcasts, emergency operational alerts, non-repudiation tracking, and compliance stats.

#### 10.1 GET `/api/v1/announcements`
- **Purpose**: Retrieve paginated feed of published announcements targeted to current employee.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Query Parameters**:
  - `page` (int, default: 0)
  - `size` (int, default: 20)
  - `priority` (`NORMAL`, `IMPORTANT`, `URGENT`, `EMERGENCY`)
  - `isAcknowledged` (boolean: filter by user's acknowledgement status)
  - `status` (`PUBLISHED`, `DRAFT`, `ARCHIVED` - `DRAFT` only visible to authors/admins)
- **Response**: `200 OK` with `PageResponse<AnnouncementSummaryResponse>`

#### 10.2 GET `/api/v1/announcements/{id}`
- **Purpose**: View announcement details. Automatically records/updates `read_at` in `announcement_reads`.
- **Authentication**: Bearer Token
- **Authorization**: Target Audience Member or Author/Admin
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `AnnouncementDetailResponse` (includes user's `isRead`, `isAcknowledged`, `acknowledgedAt`)
- **Error Codes**: `403 UNAUTHORIZED`, `404 RESOURCE_NOT_FOUND`

#### 10.3 POST `/api/v1/announcements`
- **Purpose**: Draft an enterprise announcement.
- **Authentication**: Bearer Token
- **Authorization**: `CREATE_ANNOUNCEMENT` (SUPER_ADMIN, HR_ADMIN, MANAGER for own dept, TEAM_LEADER for own team)
- **Request Body**: `CreateAnnouncementRequest` (title, content, priority, audienceType: `ALL`, `DEPARTMENT`, `TEAM`, `ROLE`, `INDIVIDUAL`, departmentId, teamId, targetRoleId, targetLocation, requiresAcknowledgement, expiresAt)
- **Response**: `201 CREATED` with `AnnouncementDetailResponse`

#### 10.4 PATCH `/api/v1/announcements/{id}`
- **Purpose**: Update an announcement draft or published notice.
- **Authentication**: Bearer Token
- **Authorization**: `EDIT_ANNOUNCEMENT` (Author, HR_ADMIN, SUPER_ADMIN)
- **Path Parameters**: `id` (UUID)
- **Request Body**: `UpdateAnnouncementRequest`
- **Response**: `200 OK` with `AnnouncementDetailResponse`

#### 10.5 POST `/api/v1/announcements/{id}/publish`
- **Purpose**: Publish draft announcement and dispatch push/in-app notifications to target audience.
- **Authentication**: Bearer Token
- **Authorization**: `CREATE_ANNOUNCEMENT` or `EDIT_ANNOUNCEMENT`
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `AnnouncementDetailResponse`
- **Error Codes**: `412 PRECONDITION_FAILED` (Already published or archived)

#### 10.6 POST `/api/v1/announcements/{id}/acknowledge`
- **Purpose**: Idempotently record employee's formal sign-off and acknowledgement of a mandatory notice.
- **Idempotency**: Multiple calls update `acknowledged_at` if not already set and return `200 OK` with current acknowledgement state without throwing duplicate key errors.
- **Authentication**: Bearer Token
- **Authorization**: Target Audience Member
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `AnnouncementAcknowledgementResponse` (announcementId, userId, acknowledgedAt)
- **Error Codes**: `404 RESOURCE_NOT_FOUND`

#### 10.7 GET `/api/v1/announcements/{id}/statistics`
- **Purpose**: HR/Manager compliance report showing read and acknowledgement metrics across target audience.
- **Authentication**: Bearer Token
- **Authorization**: Announcement Author, `HR_ADMIN`, `SUPER_ADMIN`, or Department Manager
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `AnnouncementStatisticsResponse` (totalTargeted, readCount, acknowledgedCount, acknowledgedPercentage, pendingEmployeesList)

---

### Domain 11: Documents API (`/api/v1/documents`)

Operational SOPs, driver manuals, compliance documents, and secure S3/MinIO pre-signed upload handshakes.

#### 11.1 GET `/api/v1/documents`
- **Purpose**: Search and filter company document repository according to role and departmental visibility.
- **Authentication**: Bearer Token
- **Authorization**: `VIEW_DOCUMENTS`
- **Query Parameters**: `page`, `size`, `departmentId`, `teamId`, `visibility` (`PUBLIC`, `INTERNAL`, `DEPARTMENT`, `TEAM`, `CONFIDENTIAL`), `fileType`, `search`
- **Response**: `200 OK` with `PageResponse<DocumentSummaryResponse>`

#### 11.2 GET `/api/v1/documents/{id}`
- **Purpose**: Retrieve document metadata and generate a temporary Pre-Signed Download URL (expires in 15 minutes).
- **Authentication**: Bearer Token
- **Authorization**: `VIEW_DOCUMENTS` + Authorized Visibility Scope
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `DocumentDetailResponse` (includes `downloadUrl` with pre-signed token)
- **Error Codes**: `403 UNAUTHORIZED`, `404 RESOURCE_NOT_FOUND`

#### 11.3 POST `/api/v1/documents/upload-intent`
- **Purpose**: Step 1 of Secure Object Upload Protocol: Register intent to upload a document and obtain an S3/MinIO Pre-Signed PUT URL.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_DOCUMENTS`
- **Request Body**: `DocumentUploadIntentRequest` (fileName, fileType, fileSize, checksum, departmentId, teamId, visibility, description)
- **Response**: `200 OK` with `DocumentUploadIntentResponse` (documentId, storageKey, uploadUrl, headersRequired, expiresAt)
- **Error Codes**: `422 VALIDATION_FAILED` (File size exceeds limit or forbidden MIME type)

#### 11.4 POST `/api/v1/documents/{id}/complete-upload`
- **Purpose**: Step 2 of Secure Object Upload Protocol: Client confirms binary upload to S3/MinIO completed; server validates object existence and activates document.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_DOCUMENTS` (Document Uploader)
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `DocumentDetailResponse`
- **Error Codes**: `400 BAD_REQUEST` (Object not found in storage bucket or checksum mismatch)

#### 11.5 PATCH `/api/v1/documents/{id}`
- **Purpose**: Update document metadata, title, description, or visibility classification.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_DOCUMENTS` (Uploader or Admin)
- **Path Parameters**: `id` (UUID)
- **Request Body**: `UpdateDocumentRequest`
- **Response**: `200 OK` with `DocumentDetailResponse`

#### 11.6 DELETE `/api/v1/documents/{id}`
- **Purpose**: Soft-delete/archive a document from active library.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_DOCUMENTS` (Uploader or Admin)
- **Path Parameters**: `id` (UUID)
- **Response**: `204 NO CONTENT`

---

### Domain 12: Meetings & Calendar API (`/api/v1/meetings`, `/api/v1/calendar`)

Operational shift briefings, video calls, team syncs, and RSVP tracking.

#### 12.1 GET `/api/v1/meetings`
- **Purpose**: List upcoming and past meetings where user is organizer or participant.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Query Parameters**: `page`, `size`, `startDate`, `endDate`, `status` (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
- **Response**: `200 OK` with `PageResponse<MeetingSummaryResponse>`

#### 12.2 GET `/api/v1/meetings/{id}`
- **Purpose**: Retrieve meeting details, link, organizer, and participant RSVP list.
- **Authentication**: Bearer Token
- **Authorization**: Meeting Organizer, Participant, or Dept Admin
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `MeetingDetailResponse`
- **Error Codes**: `403 UNAUTHORIZED`, `404 RESOURCE_NOT_FOUND`

#### 12.3 POST `/api/v1/meetings`
- **Purpose**: Schedule an operational meeting or shift sync.
- **Authentication**: Bearer Token
- **Authorization**: `CREATE_MEETING`
- **Request Body**: `CreateMeetingRequest` (title, description, startTime, endTime, location, meetingLink, departmentId, teamId, participantUserIds)
- **Response**: `201 CREATED` with `MeetingDetailResponse`
- **Error Codes**: `400 BAD_REQUEST` (`endTime <= startTime`), `422 VALIDATION_FAILED`

#### 12.4 PATCH `/api/v1/meetings/{id}`
- **Purpose**: Reschedule meeting, update location, or update description.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_MEETING` (Meeting Organizer or Admin)
- **Path Parameters**: `id` (UUID)
- **Request Body**: `UpdateMeetingRequest`
- **Response**: `200 OK` with `MeetingDetailResponse`

#### 12.5 DELETE `/api/v1/meetings/{id}`
- **Purpose**: Cancel a scheduled meeting and notify participants.
- **Authentication**: Bearer Token
- **Authorization**: `MANAGE_MEETING` (Organizer or Admin)
- **Path Parameters**: `id` (UUID)
- **Response**: `204 NO CONTENT`

#### 12.6 POST `/api/v1/meetings/{id}/participants`
- **Purpose**: Invite additional participants to a meeting.
- **Authentication**: Bearer Token
- **Authorization**: Meeting Organizer
- **Path Parameters**: `id` (UUID)
- **Request Body**: `AddMeetingParticipantsRequest` (userIds)
- **Response**: `200 OK` with `StandardResponse<List<MeetingParticipantResponse>>`

#### 12.7 DELETE `/api/v1/meetings/{id}/participants/{userId}`
- **Purpose**: Remove an invitee from a meeting.
- **Authentication**: Bearer Token
- **Authorization**: Meeting Organizer or Self
- **Path Parameters**: `id` (UUID), `userId` (UUID)
- **Response**: `204 NO CONTENT`

#### 12.8 PATCH `/api/v1/meetings/{id}/participants/{userId}/response`
- **Purpose**: Submit RSVP status (`ACCEPTED`, `DECLINED`, `TENTATIVE`).
- **Authentication**: Bearer Token
- **Authorization**: Self (`userId == current_user.id`)
- **Path Parameters**: `id` (UUID), `userId` (UUID)
- **Request Body**: `MeetingRsvpRequest` (response: `ACCEPTED`, `DECLINED`, `TENTATIVE`)
- **Response**: `200 OK` with `MeetingParticipantResponse`

#### 12.9 GET `/api/v1/calendar/events`
- **Purpose**: Aggregated schedule feed combining meetings, shift handovers, and department milestones within a specified date window.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Query Parameters**: `startDate` (ISO-8601), `endDate` (ISO-8601)
- **Response**: `200 OK` with `StandardResponse<List<CalendarEventResponse>>`

---

### Domain 13: Notifications API (`/api/v1/notifications`)

User activity notifications, badge counters, and read tracking.

#### 13.1 GET `/api/v1/notifications`
- **Purpose**: List user notifications with unread count.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Query Parameters**: `page`, `size`, `isRead` (boolean)
- **Response**: `200 OK` with `NotificationListResponse` (unreadCount, items, pagination)

#### 13.2 PATCH `/api/v1/notifications/{id}/read`
- **Purpose**: Mark a single notification as read.
- **Authentication**: Bearer Token
- **Authorization**: Self (`notification.user_id == current_user.id`)
- **Path Parameters**: `id` (UUID)
- **Response**: `200 OK` with `NotificationResponse`

#### 13.3 POST `/api/v1/notifications/read-all`
- **Purpose**: Mark all unread notifications for current user as read in a single batch.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Response**: `200 OK` with `StandardResponse<BatchReadResponse>` (countMarkedRead)

---

### Domain 14: Search API (`/api/v1/search`)

Unified cross-domain enterprise search.

#### 14.1 GET `/api/v1/search`
- **Purpose**: Execute unified search across employees, messages, announcements, documents, channels, and meetings.
- **Authentication**: Bearer Token
- **Authorization**: Authenticated User
- **Query Parameters**:
  - `q` (string, required, min 2 chars)
  - `category` (optional: `all`, `employees`, `messages`, `announcements`, `documents`, `channels`, `meetings`)
  - `limit` (int, default: 10 per category)
- **Response**: `200 OK` with `SearchResultsResponse` (categorized matches with snippet highlights)

---

### Domain 15: Admin & System Metrics API (`/api/v1/admin`)

Administrative cockpit, system telemetry, and platform configurations.

#### 15.1 GET `/api/v1/admin/dashboard`
- **Purpose**: Fetch executive operational dashboard metrics.
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN` or `HR_ADMIN`
- **Response**: `200 OK` with `AdminDashboardResponse` (activeUsersCount, totalEmployeesCount, messageThroughput24h, unacknowledgedUrgentAnnouncementsCount, storageUsageBytes, systemHealthStatus)

#### 15.2 GET `/api/v1/admin/system/health`
- **Purpose**: Infrastructure health probe for database, Redis/WebSocket cache, and object storage connectivity.
- **Authentication**: Bearer Token
- **Authorization**: `SUPER_ADMIN`
- **Response**: `200 OK` with `SystemHealthResponse` (status, dbLatencyMs, storageStatus, activeSessions)

---

### Domain 16: Audit Trail API (`/api/v1/admin/audit-logs`)

Immutable compliance and security activity log exploration.

#### 16.1 GET `/api/v1/admin/audit-logs`
- **Purpose**: Search and filter append-only security and operational audit logs. Normal users are strictly forbidden from accessing audit logs.
- **Authentication**: Bearer Token
- **Authorization**: `VIEW_AUDIT_LOGS` (SUPER_ADMIN, HR_ADMIN for employee events)
- **Query Parameters**:
  - `page` (int, default: 0)
  - `size` (int, default: 50, max: 100)
  - `actorId` (UUID)
  - `action` (string: e.g., `USER_LOGIN`, `USER_LOCKED`, `EMPLOYEE_OFFBOARDED`, `ANNOUNCEMENT_ACKNOWLEDGED`, `PERMISSION_CHANGED`)
  - `entityType` (string: `USER`, `EMPLOYEE`, `ANNOUNCEMENT`, `DOCUMENT`, `ROLE`)
  - `entityId` (UUID)
  - `startDate` (ISO-8601)
  - `endDate` (ISO-8601)
  - `sort` (default: `createdAt,desc`)
- **Response**: `200 OK` with `PageResponse<AuditLogResponse>`
- **Error Codes**: `403 UNAUTHORIZED`
- **Immutability Guarantee**: This API is strictly read-only. No `POST`, `PUT`, `PATCH`, or `DELETE` endpoints exist.

---

## 4. End-to-End Object Storage Upload Handshake

```
+----------------+              +-----------------+             +--------------------+
|  Client / App  |              |  LogiConnect    |             | S3 / MinIO Object  |
|  (Web / Mobile)|              |  Backend API    |             | Storage Service    |
+-------+--------+              +--------+--------+             +---------+----------+
        |                                |                                |
        | 1. POST /documents/upload-intent                               |
        |    (fileName, size, SHA256)    |                                |
        +------------------------------->|                                |
        |                                | 2. Generate pre-signed PUT URL |
        |                                |    & store PENDING doc record  |
        | 3. Returns Pre-Signed PUT URL  |                                |
        |    + headers + storageKey      |                                |
        |<-------------------------------+                                |
        |                                                                 |
        | 4. Direct HTTP PUT Binary (with checksum & content-type)        |
        +---------------------------------------------------------------->|
        |                                                                 |
        | 5. 200 OK (Upload stored directly in bucket)                    |
        |<----------------------------------------------------------------+
        |                                |                                |
        | 6. POST /documents/{id}/complete-upload                         |
        +------------------------------->|                                |
        |                                | 7. Verify object metadata &    |
        |                                |    mark document ACTIVE        |
        | 8. 200 OK DocumentDetail       |                                |
        |<-------------------------------+                                |
```

This ensures application nodes never bottleneck on large binary streams or consume JVM heap with multipart file buffers.
