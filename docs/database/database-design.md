# LogiConnect Enterprise Database Architecture & Design

## 1. Database Purpose & Overview

**LogiConnect** is an enterprise-grade internal communication and collaboration platform designed for a logistics company with 2,000+ employees across regional hubs, fulfillment centers, transport lines, and corporate headquarters.

The primary mission is to provide an authorized, secure, and auditable communication infrastructure that eliminates company reliance on unmanaged consumer tools (such as WhatsApp groups). It guarantees:
- Strict corporate identity and organizational hierarchy enforcement (Company &rarr; Department &rarr; Team &rarr; Employee).
- Granular Role-Based Access Control (RBAC).
- Dynamic channel authorization aligned with organizational structure.
- Guaranteed delivery, reading, and non-repudiation tracking for corporate announcements and emergency operational broadcasts.
- High-throughput direct and team communication channels with rich attachments and thread support.
- Comprehensive security and immutable compliance audit logging.

---

## 2. Architectural Design Principles

1. **Storage Engine**: PostgreSQL 14+ / 16 (Relational ACID Compliance, JSONB support for semi-structured metadata, Full Text Search capabilities).
2. **Primary Key Strategy**: UUIDv4 (`gen_random_uuid()`) for all entities to prevent enumeration attacks, simplify distributed ID generation, and support multi-region or sharded data synchronization in future iterations.
3. **Time Standard**: All timestamps are stored in UTC with timezone awareness using `TIMESTAMPTZ`. Automatic `updated_at` timestamps are managed via database triggers.
4. **Binary & Document Storage**: Zero blob/binary payload in PostgreSQL. Documents and attachments store object storage references (`storage_key`, `file_url`, `file_size`, `checksum`) targeting S3/MinIO/GCS.
5. **Spring Data JPA & Hibernate Compatibility**: Standard snake_case column names, non-reserved keywords, composite keys mapped via `@Embeddable` / `@IdClass`, and explicit foreign keys.
6. **Soft Deletion & Enterprise Retention**: Critical business communication and audit trails employ soft deletion (`deleted_at`, `status`) to comply with corporate retention and dispute resolution policies.
7. **Authoritative Migration Source**: The single authoritative Flyway migration is located in `backend/src/main/resources/db/migration/V1__initial_schema.sql`.

---

## 3. Database Schema Specification

The schema consists of **21 tables** organized across **9 business domains**.

```
LogiConnect Relational Schema (21 Tables)
├── 1. Organizational Structure Domain (3 Tables)
│   ├── departments
│   ├── teams
│   └── employees
├── 2. Authentication & Access Control Domain (5 Tables)
│   ├── users
│   ├── roles
│   ├── permissions
│   ├── user_roles
│   └── role_permissions
├── 3. Communication & Messaging Domain (6 Tables)
│   ├── conversations
│   ├── conversation_members
│   ├── channels
│   ├── channel_members
│   ├── messages
│   └── message_attachments
├── 4. Company Broadcast Domain (2 Tables)
│   ├── announcements
│   └── announcement_reads
├── 5. Document Management Domain (1 Table)
│   └── documents
├── 6. Calendar & Collaboration Domain (2 Tables)
│   ├── meetings
│   └── meeting_participants
├── 7. Notification Domain (1 Table)
│   └── notifications
└── 8. Enterprise Audit & Compliance Domain (1 Table)
    └── audit_logs
```

---

### Domain 1: Organizational Structure (3 Tables)

#### `departments`
Represents top-level corporate divisions (Operations, Warehouse, Customer Support, IT, HR, Finance).
- `id` (UUID, PK): Unique department identifier.
- `code` (VARCHAR(50), NOT NULL, UNIQUE): Machine-readable business code (e.g. `DEPT-OPS`).
- `name` (VARCHAR(100), NOT NULL, UNIQUE): Human-readable department name.
- `description` (TEXT): Overview of department responsibilities.
- `manager_id` (UUID, FK &rarr; `employees.id`): Employee designated as department head.
- `status` (VARCHAR(20), NOT NULL, DEFAULT `'ACTIVE'`): `'ACTIVE'`, `'INACTIVE'`, `'ARCHIVED'`.
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

#### `teams`
Sub-units or geographical operational units within a department (e.g., Bangalore Hub, Mumbai Support).
- `id` (UUID, PK): Unique team identifier.
- `department_id` (UUID, NOT NULL, FK &rarr; `departments.id` ON DELETE CASCADE): Parent department.
- `code` (VARCHAR(50), NOT NULL, UNIQUE): Machine-readable team code (e.g. `TEAM-OPS-BLR`).
- `name` (VARCHAR(100), NOT NULL): Team title.
- `description` (TEXT): Operational focus.
- `team_lead_id` (UUID, FK &rarr; `employees.id`): Designated lead or shift supervisor.
- `status` (VARCHAR(20), NOT NULL, DEFAULT `'ACTIVE'`): `'ACTIVE'`, `'INACTIVE'`, `'ARCHIVED'`.
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).
- **Constraints**: `UNIQUE (department_id, name)`.

#### `employees`
HR-centric directory profile of every worker in the logistics enterprise.
- `id` (UUID, PK): Unique employee ID.
- `employee_code` (VARCHAR(50), NOT NULL, UNIQUE): Corporate employee badge code (e.g. `EMP-1001`).
- `first_name` / `last_name` (VARCHAR(100), NOT NULL): Legal name.
- `email` (VARCHAR(255), NOT NULL, UNIQUE): Official corporate email.
- `phone` (VARCHAR(30)): Contact phone number.
- `profile_photo_url` (VARCHAR(1000)): CDN/S3 photo path.
- `designation` (VARCHAR(100), NOT NULL): Job role (e.g. "Senior Dispatch Coordinator").
- `department_id` (UUID, NOT NULL, FK &rarr; `departments.id` ON DELETE RESTRICT).
- `team_id` (UUID, FK &rarr; `teams.id` ON DELETE SET NULL).
- `manager_id` (UUID, FK &rarr; `employees.id` ON DELETE SET NULL): Hierarchical supervisor.
- `location` (VARCHAR(100), NOT NULL): Hub/Fulfillment/HQ location (e.g. "Bangalore Hub").
- `joining_date` (DATE, NOT NULL): Employment start date.
- `exit_date` (DATE): Offboarding / resignation date.
- `status` (VARCHAR(20), NOT NULL, DEFAULT `'ACTIVE'`): `'ACTIVE'`, `'PROBATION'`, `'ON_LEAVE'`, `'SUSPENDED'`, `'TERMINATED'`, `'RESIGNED'`.
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

---

### Domain 2: Authentication & Access Control (5 Tables)

#### `users`
Security principal storing credentials, authentication flags, and session security state.
- `id` (UUID, PK): Unique user account identifier.
- `employee_id` (UUID, NOT NULL, UNIQUE, FK &rarr; `employees.id` ON DELETE CASCADE): Strict 1-to-1 link ensuring no multiple accounts reference the same employee.
- `email` (VARCHAR(255), NOT NULL, UNIQUE): Login username / email identifier.
- `password_hash` (VARCHAR(255), NOT NULL): Cryptographically hashed password (BCrypt/Argon2). Never plain text.
- `status` (VARCHAR(20), NOT NULL, DEFAULT `'ACTIVE'`): `'ACTIVE'`, `'INACTIVE'`, `'LOCKED'`, `'PENDING_VERIFICATION'`.
- `failed_login_attempts` (INT, NOT NULL, DEFAULT 0): Account lockout prevention metric.
- `locked_until` (TIMESTAMPTZ): Lockout release timestamp.
- `last_login_at` (TIMESTAMPTZ): Last successful authentication timestamp.
- `password_changed_at` (TIMESTAMPTZ): Password rotation policy tracking.
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

#### `roles`
Named permission groupings.
- `id` (UUID, PK): Unique role identifier.
- `name` (VARCHAR(50), NOT NULL, UNIQUE): System code (`SUPER_ADMIN`, `HR_ADMIN`, `MANAGER`, `TEAM_LEADER`, `EMPLOYEE`).
- `display_name` (VARCHAR(100), NOT NULL): UI label.
- `description` (TEXT): Scope explanation.
- `is_system` (BOOLEAN, NOT NULL, DEFAULT FALSE): Prevents accidental deletion of built-in roles.
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

#### `permissions`
Fine-grained authorization capabilities.
- `id` (UUID, PK): Unique permission identifier.
- `name` (VARCHAR(100), NOT NULL, UNIQUE): E.g. `MANAGE_EMPLOYEES`, `CREATE_ANNOUNCEMENT`, `CREATE_CHANNEL`.
- `display_name` (VARCHAR(150), NOT NULL).
- `module` (VARCHAR(50), NOT NULL): Grouping (`EMPLOYEE`, `ANNOUNCEMENT`, `CHANNEL`, `CONVERSATION`, `DOCUMENT`, `MEETING`, `AUDIT`, `SYSTEM`).
- `description` (TEXT).
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

#### `user_roles`
Many-to-many junction between users and roles.
- `user_id` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE CASCADE).
- `role_id` (UUID, NOT NULL, FK &rarr; `roles.id` ON DELETE CASCADE).
- `assigned_at` (TIMESTAMPTZ, NOT NULL).
- `assigned_by` (UUID, FK &rarr; `users.id`).
- **Primary Key**: `(user_id, role_id)`.

#### `role_permissions`
Many-to-many junction between roles and granular permissions.
- `role_id` (UUID, NOT NULL, FK &rarr; `roles.id` ON DELETE CASCADE).
- `permission_id` (UUID, NOT NULL, FK &rarr; `permissions.id` ON DELETE CASCADE).
- `assigned_at` (TIMESTAMPTZ, NOT NULL).
- **Primary Key**: `(role_id, permission_id)`.

#### `user_sessions`
Active refresh token sessions tracking authentication state, rotation, and revocation.
- `id` (UUID, PK): Unique session ID.
- `user_id` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE CASCADE): User owning this session.
- `refresh_token_hash` (VARCHAR(64), NOT NULL, UNIQUE): SHA-256 hash of issued refresh token (raw token is never stored in DB).
- `device_info` (VARCHAR(255)): User device, OS, or browser identifier.
- `ip_address` (VARCHAR(45)): Client IP address (IPv4/IPv6).
- `expires_at` (TIMESTAMPTZ, NOT NULL): Refresh token expiration timestamp.
- `revoked_at` (TIMESTAMPTZ): Revocation timestamp (for explicit logout or rotation replay detection).
- `last_used_at` (TIMESTAMPTZ, NOT NULL): Timestamp when token was last refreshed.
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

---

### Domain 3: Communication & Messaging (6 Tables)

#### `conversations`
Direct (1-to-1) and ad-hoc multi-member chat groups.
- `id` (UUID, PK).
- `type` (VARCHAR(20), NOT NULL): `'DIRECT'` (between 2 users) or `'GROUP'` (custom ad-hoc group).
- `name` (VARCHAR(150)): Optional custom name for group chats.
- `description` (TEXT).
- `avatar_url` (VARCHAR(1000)).
- `created_by` (UUID, FK &rarr; `users.id` ON DELETE SET NULL).
- `is_archived` (BOOLEAN, NOT NULL, DEFAULT FALSE).
- `last_message_at` (TIMESTAMPTZ): Denormalized index column to order conversation inbox queries efficiently.
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

#### `conversation_members`
Membership in direct and group conversations.
- `conversation_id` (UUID, NOT NULL, FK &rarr; `conversations.id` ON DELETE CASCADE).
- `user_id` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE CASCADE).
- `role` (VARCHAR(20), NOT NULL, DEFAULT `'MEMBER'`): `'MEMBER'`, `'ADMIN'`.
- `joined_at` (TIMESTAMPTZ, NOT NULL).
- `left_at` (TIMESTAMPTZ): Retained for message history visibility bounds.
- `last_read_at` (TIMESTAMPTZ): Used to calculate unread message badges.
- `is_muted` (BOOLEAN, NOT NULL, DEFAULT FALSE).
- `is_pinned` (BOOLEAN, NOT NULL, DEFAULT FALSE).
- **Primary Key**: `(conversation_id, user_id)`.

#### `channels`
Structured organizational workspaces aligned with departments, hubs, or company-wide audiences.
- `id` (UUID, PK).
- `name` (VARCHAR(100), NOT NULL): E.g. `#bangalore-hub-ops`.
- `slug` (VARCHAR(120), NOT NULL, UNIQUE): URL-safe identifier.
- `description` (TEXT).
- `type` (VARCHAR(20), NOT NULL): `'COMPANY'`, `'DEPARTMENT'`, `'TEAM'`, `'PRIVATE'`.
- `department_id` (UUID, FK &rarr; `departments.id` ON DELETE SET NULL).
- `team_id` (UUID, FK &rarr; `teams.id` ON DELETE SET NULL).
- `created_by` (UUID, FK &rarr; `users.id` ON DELETE SET NULL).
- `status` (VARCHAR(20), NOT NULL, DEFAULT `'ACTIVE'`): `'ACTIVE'`, `'ARCHIVED'`, `'DELETED'`.
- `is_read_only` (BOOLEAN, NOT NULL, DEFAULT FALSE): Broadcast-only channel restriction.
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

#### `channel_members`
Explicit membership records for private/restricted channels.
- `channel_id` (UUID, NOT NULL, FK &rarr; `channels.id` ON DELETE CASCADE).
- `user_id` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE CASCADE).
- `role` (VARCHAR(20), NOT NULL, DEFAULT `'MEMBER'`): `'MEMBER'`, `'MODERATOR'`, `'ADMIN'`.
- `joined_at` (TIMESTAMPTZ, NOT NULL).
- `last_read_at` (TIMESTAMPTZ).
- `is_muted` (BOOLEAN, NOT NULL, DEFAULT FALSE).
- **Primary Key**: `(channel_id, user_id)`.

#### `messages`
Unified message entity for direct chats, group rooms, and channels.
- `id` (UUID, PK).
- `conversation_id` (UUID, FK &rarr; `conversations.id` ON DELETE CASCADE).
- `channel_id` (UUID, FK &rarr; `channels.id` ON DELETE CASCADE).
- `sender_id` (UUID, FK &rarr; `users.id` ON DELETE SET NULL).
- `message_type` (VARCHAR(20), NOT NULL, DEFAULT `'TEXT'`): `'TEXT'`, `'FILE'`, `'IMAGE'`, `'SYSTEM'`, `'LOCATION'`, `'AUDIO'`, `'VIDEO'`.
- `content` (TEXT).
- `reply_to_message_id` (UUID, FK &rarr; `messages.id` ON DELETE SET NULL): Threaded replying.
- `is_pinned` (BOOLEAN, NOT NULL, DEFAULT FALSE).
- `created_at` (TIMESTAMPTZ, NOT NULL).
- `edited_at` (TIMESTAMPTZ): Populated when sender updates content.
- `deleted_at` (TIMESTAMPTZ): Soft delete preserving conversation timeline.
- **Integrity Constraint**:
  ```sql
  CONSTRAINT chk_message_container CHECK (
      (conversation_id IS NOT NULL AND channel_id IS NULL) OR
      (conversation_id IS NULL AND channel_id IS NOT NULL)
  )
  ```

#### `message_attachments`
Rich media files, PDF delivery manifests, and images attached to messages (metadata only, no binary blobs in DB).
- `id` (UUID, PK).
- `message_id` (UUID, NOT NULL, FK &rarr; `messages.id` ON DELETE CASCADE).
- `storage_key` (VARCHAR(500), NOT NULL).
- `file_url` (VARCHAR(1000), NOT NULL).
- `file_name` (VARCHAR(255), NOT NULL).
- `file_type` (VARCHAR(100), NOT NULL): MIME type.
- `file_size` (BIGINT, NOT NULL): Size in bytes.
- `created_at` (TIMESTAMPTZ, NOT NULL).

---

### Domain 4: Company Broadcasts & Announcements (2 Tables)

#### `announcements`
Top-down enterprise broadcasts, health & safety notices, operational alerts.
- `id` (UUID, PK).
- `title` (VARCHAR(255), NOT NULL).
- `content` (TEXT, NOT NULL).
- `created_by` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE RESTRICT).
- `priority` (VARCHAR(20), NOT NULL, DEFAULT `'NORMAL'`): `'NORMAL'`, `'IMPORTANT'`, `'URGENT'`, `'EMERGENCY'`.
- `audience_type` (VARCHAR(30), NOT NULL, DEFAULT `'ALL'`): `'ALL'`, `'DEPARTMENT'`, `'TEAM'`, `'ROLE'`, `'INDIVIDUAL'`, `'ALL_EMPLOYEES'`.
- `department_id` (UUID, FK &rarr; `departments.id` ON DELETE SET NULL).
- `team_id` (UUID, FK &rarr; `teams.id` ON DELETE SET NULL).
- `target_role_id` (UUID, FK &rarr; `roles.id` ON DELETE SET NULL).
- `target_location` (VARCHAR(100)): Target specific logistics hub or regional office.
- `published_at` (TIMESTAMPTZ): Broadcast publication timestamp.
- `expires_at` (TIMESTAMPTZ): Auto-archival cutoff.
- `requires_acknowledgement` (BOOLEAN, NOT NULL, DEFAULT FALSE): Mandatory employee sign-off flag.
- `status` (VARCHAR(20), NOT NULL, DEFAULT `'DRAFT'`): `'DRAFT'`, `'PUBLISHED'`, `'ARCHIVED'`, `'CANCELLED'`.
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

#### `announcement_reads`
Non-repudiation audit table tracking who read and acknowledged official notices.
- `id` (UUID, PK).
- `announcement_id` (UUID, NOT NULL, FK &rarr; `announcements.id` ON DELETE CASCADE).
- `user_id` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE CASCADE).
- `read_at` (TIMESTAMPTZ, NOT NULL, DEFAULT CURRENT_TIMESTAMP).
- `acknowledged_at` (TIMESTAMPTZ): Explicit acknowledgement timestamp.
- **Constraints**: `UNIQUE (announcement_id, user_id)` (Guarantees exactly one record per employee per notice).

---

### Domain 5: Document Management (1 Table)

#### `documents`
Repository for operational SOPs, safety guidelines, compliance policies, and templates.
- `id` (UUID, PK).
- `name` (VARCHAR(255), NOT NULL).
- `description` (TEXT).
- `storage_key` (VARCHAR(500), NOT NULL, UNIQUE): Object storage key.
- `file_url` (VARCHAR(1000), NOT NULL): Presigned / secure URL.
- `file_type` (VARCHAR(100), NOT NULL): MIME type (e.g. `application/pdf`).
- `file_size` (BIGINT, NOT NULL): File size in bytes.
- `checksum` (VARCHAR(64)): SHA-256 integrity hash.
- `uploaded_by` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE RESTRICT).
- `department_id` (UUID, FK &rarr; `departments.id` ON DELETE SET NULL).
- `team_id` (UUID, FK &rarr; `teams.id` ON DELETE SET NULL).
- `visibility` (VARCHAR(20), NOT NULL, DEFAULT `'INTERNAL'`): `'PUBLIC'`, `'INTERNAL'`, `'DEPARTMENT'`, `'TEAM'`, `'CONFIDENTIAL'`.
- `status` (VARCHAR(20), NOT NULL, DEFAULT `'ACTIVE'`): `'ACTIVE'`, `'ARCHIVED'`, `'DELETED'`.
- `deleted_at` (TIMESTAMPTZ).
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).

---

### Domain 6: Calendar & Collaboration (2 Tables)

#### `meetings`
Shift handover syncs, driver briefings, and regional management calls.
- `id` (UUID, PK).
- `title` (VARCHAR(255), NOT NULL).
- `description` (TEXT).
- `organizer_id` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE RESTRICT).
- `start_time` (TIMESTAMPTZ, NOT NULL).
- `end_time` (TIMESTAMPTZ, NOT NULL).
- `location` (VARCHAR(255)): Meeting room or facility name.
- `meeting_link` (VARCHAR(1000)): Jitsi / Zoom / Teams link.
- `status` (VARCHAR(20), NOT NULL, DEFAULT `'SCHEDULED'`): `'SCHEDULED'`, `'IN_PROGRESS'`, `'COMPLETED'`, `'CANCELLED'`, `'RESCHEDULED'`.
- `department_id` (UUID, FK &rarr; `departments.id` ON DELETE SET NULL).
- `team_id` (UUID, FK &rarr; `teams.id` ON DELETE SET NULL).
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).
- **Constraints**: `CHECK (end_time > start_time)`.

#### `meeting_participants`
RSVP and attendance status for invited personnel.
- `meeting_id` (UUID, NOT NULL, FK &rarr; `meetings.id` ON DELETE CASCADE).
- `user_id` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE CASCADE).
- `response` (VARCHAR(20), NOT NULL, DEFAULT `'PENDING'`): `'PENDING'`, `'ACCEPTED'`, `'DECLINED'`, `'TENTATIVE'`.
- `joined_at` (TIMESTAMPTZ).
- `created_at` / `updated_at` (TIMESTAMPTZ, NOT NULL).
- **Primary Key**: `(meeting_id, user_id)`.

---

### Domain 7: Notifications (1 Table)

#### `notifications`
In-app alerts and mobile notification queues.
- `id` (UUID, PK).
- `user_id` (UUID, NOT NULL, FK &rarr; `users.id` ON DELETE CASCADE).
- `type` (VARCHAR(50), NOT NULL): `ANNOUNCEMENT`, `MESSAGE`, `MEETING_INVITE`, `MENTION`, `SYSTEM`.
- `title` (VARCHAR(255), NOT NULL).
- `message` (TEXT, NOT NULL).
- `reference_type` (VARCHAR(50)): E.g. `ANNOUNCEMENT`, `CONVERSATION`, `CHANNEL`, `MEETING`.
- `reference_id` (UUID): ID of the referenced entity.
- `is_read` (BOOLEAN, NOT NULL, DEFAULT FALSE).
- `read_at` (TIMESTAMPTZ).
- `created_at` (TIMESTAMPTZ, NOT NULL).

---

### Domain 8: Enterprise Security & Audit Trail (1 Table)

#### `audit_logs`
Append-only, immutable compliance and security activity log.
- `id` (UUID, PK).
- `actor_id` (UUID, FK &rarr; `users.id` ON DELETE SET NULL).
- `action` (VARCHAR(100), NOT NULL): E.g. `USER_LOGIN`, `USER_LOCKED`, `EMPLOYEE_OFFBOARDED`, `ANNOUNCEMENT_ACKNOWLEDGED`, `PERMISSION_CHANGED`.
- `entity_type` (VARCHAR(50), NOT NULL): E.g. `USER`, `EMPLOYEE`, `ANNOUNCEMENT`, `DOCUMENT`, `ROLE`.
- `entity_id` (UUID).
- `ip_address` (VARCHAR(45)): IPv4 or IPv6.
- `user_agent` (TEXT): Browser/Client identifier.
- `metadata` (JSONB): Structured contextual details (before/after snapshots, request parameters).
- `created_at` (TIMESTAMPTZ, NOT NULL, DEFAULT CURRENT_TIMESTAMP).
- **Immutability Enforcement**:
  - `INSERT` permitted.
  - `UPDATE` and `DELETE` strictly prevented via database-level trigger `trg_audit_logs_immutability`.

---

## 4. Organizational & Channel Authorization Model

The platform avoids duplicating thousands of redundant channel membership records for organizational units. Channel access is evaluated dynamically:

```
                  ┌──────────────────────┐
                  │ Channel Access Model │
                  └──────────┬───────────┘
                             │
       ┌─────────────────────┼─────────────────────┬────────────────────┐
       ▼                     ▼                     ▼                    ▼
   [ COMPANY ]        [ DEPARTMENT ]            [ TEAM ]           [ PRIVATE ]
  All Active         Employees where        Employees where     Explicit records
  Employees          employee.dept_id       employee.team_id    in channel_members
  have access        = channel.dept_id      = channel.team_id   table
```

- **Company Channels** (e.g. `#general`, `#all-hands`): Accessible company-wide to all active employees.
- **Department Channels** (e.g. `#operations-general`, `#warehouse-ops`): Dynamically accessible by checking `current_user.employee.department_id = channel.department_id`.
- **Team Channels** (e.g. `#blr-hub-dispatch`): Dynamically accessible by checking `current_user.employee.team_id = channel.team_id`.
- **Private Channels** (e.g. `#lead-execs`): Requires explicit membership records in `channel_members`.

---

## 5. Announcement Targeting Model

Announcements support granular targeting without bloated recipient tables:
- **MVP Targeting**: Evaluated via `audience_type` (`ALL`, `DEPARTMENT`, `TEAM`, `ROLE`, `INDIVIDUAL`, `ALL_EMPLOYEES`) along with `department_id`, `team_id`, `target_role_id`, and `target_location`.
- **Read & Acknowledgement Non-Repudiation**: `announcement_reads` stores exactly one record per user per announcement with `read_at` and `acknowledged_at`.
- **Future Expansion**: Direct individual and complex targeting criteria can be resolved dynamically at query time using employee directory attributes.

---

## 6. Indexing & Query Optimization Strategy

| Table | Index Name | Columns / Condition | Workload / Query Path |
| :--- | :--- | :--- | :--- |
| `employees` | `idx_employees_dept_id` | `(department_id)` | Department directory lookups |
| `employees` | `idx_employees_team_id` | `(team_id)` | Team membership filter |
| `employees` | `idx_employees_manager_id` | `(manager_id)` | Org hierarchy tree queries |
| `employees` | `idx_employees_status` | `(status)` | Active vs offboarded directory search |
| `employees` | `idx_employees_location` | `(location)` | Hub-level location broadcast/search |
| `users` | `idx_users_status` | `(status)` | Authentication gate checks |
| `conversations` | `idx_conversations_last_msg_at` | `(last_message_at DESC NULLS LAST)` | User chat inbox sorting |
| `conversation_members` | `idx_conversation_members_user_id`| `(user_id)` | "My Conversations" list |
| `channels` | `idx_channels_type_status` | `(type, status)` | Public/team channel directory |
| `channel_members` | `idx_channel_members_user_id` | `(user_id)` | "My Channels" list |
| `messages` | `idx_messages_conv_created` | `(conversation_id, created_at DESC) WHERE deleted_at IS NULL` | Paginated message feed in direct/group chats |
| `messages` | `idx_messages_chan_created` | `(channel_id, created_at DESC) WHERE deleted_at IS NULL` | Paginated message feed in channels |
| `messages` | `idx_messages_sender_id` | `(sender_id)` | User messaging activity / GDPR export |
| `messages` | `idx_messages_reply_to` | `(reply_to_message_id)` | Threaded message reply resolution |
| `announcements` | `idx_announcements_pub_status` | `(published_at DESC) WHERE status = 'PUBLISHED'` | Published announcement feed |
| `announcements` | `idx_announcements_dept_id` | `(department_id)` | Department targeted announcements |
| `announcement_reads` | `idx_announcement_reads_user_id` | `(user_id)` | User acknowledgement status checking |
| `announcement_reads` | `idx_announcement_reads_ack` | `(announcement_id, acknowledged_at)` | HR Compliance report (% acknowledged) |
| `documents` | `idx_documents_visibility_status` | `(visibility, status) WHERE status = 'ACTIVE'` | Authorized document search |
| `meetings` | `idx_meetings_start_time` | `(start_time)` | Calendar schedule range lookups |
| `meetings` | `idx_meetings_status` | `(status)` | Active/upcoming meetings filter |
| `meeting_participants`| `idx_meeting_participants_user_resp`| `(user_id, response)` | User upcoming meeting invites |
| `notifications` | `idx_notifications_user_unread`| `(user_id, created_at DESC) WHERE is_read = FALSE` | High-frequency unread badge counter |
| `notifications` | `idx_notifications_user_all` | `(user_id, created_at DESC)` | Notification history list |
| `audit_logs` | `idx_audit_logs_actor_created` | `(actor_id, created_at DESC)` | User activity audit query |
| `audit_logs` | `idx_audit_logs_created_at` | `(created_at DESC)` | Chronological audit export |
| `audit_logs` | `idx_audit_logs_metadata_gin` | `USING GIN (metadata)` | Dynamic JSONB filter (e.g. `metadata->>'ip'`) |

---

## 7. Security & Offboarding Considerations

1. **User Account Separation**:
   - `users.employee_id` is unique and mandatory (`NOT NULL UNIQUE REFERENCES employees(id)`).
   - Deactivating a user (`users.status = 'INACTIVE'`) immediately revokes access while preserving all employee directory and audit data.
2. **Audit Immutability**:
   - Application service accounts should only have `INSERT` and `SELECT` privileges on `audit_logs`.
   - The PostgreSQL trigger `trg_audit_logs_immutability` blocks any `UPDATE` or `DELETE` at the database engine level.
3. **No Stored Plaintext Credentials**:
   - Passwords stored only as salted BCrypt / Argon2 hashes.

---

## 8. Authoritative File Locations

- **Flyway Relational Schema Migration**: `backend/src/main/resources/db/migration/V1__initial_schema.sql`
- **Development Seed Data**: `database/seeds/development/seed_data.sql`
- **Mermaid ER Diagram**: `docs/database/er-diagram.mmd`
- **Architecture Documentation**: `docs/database/database-design.md`
