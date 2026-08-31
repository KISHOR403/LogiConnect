# LogiConnect Security & Authorization Matrix

## 1. Security Architecture Overview

**LogiConnect** implements a defense-in-depth security model combining:
1. **Stateless JWT Authentication**: Validates principal identity and system status on every request.
2. **Role-Based Access Control (RBAC)**: Baseline roles (`SUPER_ADMIN`, `HR_ADMIN`, `MANAGER`, `TEAM_LEADER`, `EMPLOYEE`) configured with granular permissions.
3. **Fine-Grained Permission Enforcement**: Annotations (e.g. `@PreAuthorize("hasAuthority('MANAGE_EMPLOYEES')")`) guarding service and controller operations.
4. **Dynamic Organizational & Resource-Level Authorization**: Contextual evaluation verifying department, team, channel membership, or entity ownership boundaries.

---

## 2. Global Role Hierarchy & Capabilities

```
                      ┌─────────────────┐
                      │   SUPER_ADMIN   │ (Unrestricted Platform Administration)
                      └────────┬────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
   ┌─────────────────┐                   ┌─────────────────┐
   │    HR_ADMIN     │                   │     MANAGER     │
   │ (Org Directory, │                   │ (Department     │
   │  Policy Notices)│                   │  Operations)    │
   └────────┬────────┘                   └────────┬────────┘
            │                                     │
            └──────────────────┬──────────────────┘
                               ▼
                      ┌─────────────────┐
                      │   TEAM_LEADER   │ (Hub / Shift Supervision)
                      └────────┬────────┘
                               ▼
                      ┌─────────────────┐
                      │    EMPLOYEE     │ (Core Collaboration)
                      └─────────────────┘
```

---

## 3. End-to-End API Authorization Matrix

| Domain & Endpoint | HTTP Method | Required Permission | Employee | Team Leader | Manager | HR Admin | Super Admin | Resource / Contextual Rule |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Authentication** | | | | | | | | |
| `/auth/login` | POST | *None* | Yes | Yes | Yes | Yes | Yes | Public authentication gate |
| `/auth/refresh` | POST | *None* | Yes | Yes | Yes | Yes | Yes | Valid refresh token required |
| `/auth/logout` | POST | Authenticated | Yes | Yes | Yes | Yes | Yes | Revokes active session |
| `/auth/me` | GET | Authenticated | Yes | Yes | Yes | Yes | Yes | Returns own principal info |
| `/auth/change-password` | POST | Authenticated | Yes | Yes | Yes | Yes | Yes | Validates current password |
| `/auth/forgot-password` | POST | *None* | Yes | Yes | Yes | Yes | Yes | Rate-limited public endpoint |
| `/auth/reset-password` | POST | *None* | Yes | Yes | Yes | Yes | Yes | Verified cryptographic token |
| **Employees** | | | | | | | | |
| `/employees` (Directory) | GET | `VIEW_EMPLOYEES` | Yes | Yes | Yes | Yes | Yes | Directory view (active only) |
| `/employees/{id}` | GET | `VIEW_EMPLOYEES` | Yes | Yes | Yes | Yes | Yes | Profile view |
| `/employees` | POST | `MANAGE_EMPLOYEES` | No | No | No | Yes | Yes | Corporate HR onboarding |
| `/employees/{id}` | PATCH | `MANAGE_EMPLOYEES` | Self* | Self* | Self* | Yes | Yes | *Self can only update photo/phone |
| `/employees/{id}/status` | PATCH | `MANAGE_EMPLOYEES` | No | No | No | Yes | Yes | Offboarding/status transition |
| **Departments** | | | | | | | | |
| `/departments` | GET | Authenticated | Yes | Yes | Yes | Yes | Yes | All active departments |
| `/departments/{id}` | GET | Authenticated | Yes | Yes | Yes | Yes | Yes | Department detail |
| `/departments` | POST | `MANAGE_ROLES` | No | No | No | Yes | Yes | Org restructuring |
| `/departments/{id}` | PATCH | `MANAGE_ROLES` | No | No | No | Yes | Yes | Dept updates |
| `/departments/{id}/employees`| GET | `VIEW_EMPLOYEES` | Yes | Yes | Yes | Yes | Yes | Filtered dept roster |
| **Teams** | | | | | | | | |
| `/teams` | GET | Authenticated | Yes | Yes | Yes | Yes | Yes | Team directory |
| `/teams/{id}` | GET | Authenticated | Yes | Yes | Yes | Yes | Yes | Team detail |
| `/teams` | POST | `MANAGE_CHANNEL` | No | No | Dept* | Yes | Yes | *Manager within own dept |
| `/teams/{id}` | PATCH | `MANAGE_CHANNEL` | No | No | Dept* | Yes | Yes | *Manager within own dept |
| `/teams/{id}/employees` | GET | `VIEW_EMPLOYEES` | Yes | Yes | Yes | Yes | Yes | Filtered team roster |
| **Conversations** | | | | | | | | |
| `/conversations` | GET | Authenticated | Yes | Yes | Yes | Yes | Yes | Only user's conversations |
| `/conversations/{id}` | GET | Authenticated | Member | Member | Member | Member | Member | Must be member of chat |
| `/conversations/direct` | POST | `SEND_MESSAGE` | Yes | Yes | Yes | Yes | Yes | Idempotent direct chat |
| `/conversations/group` | POST | `SEND_MESSAGE` | Yes | Yes | Yes | Yes | Yes | Multi-member group creation |
| `/conversations/{id}/members`| GET | Authenticated | Member | Member | Member | Member | Member | Member list |
| `/conversations/{id}/members`| POST | Authenticated | Admin/Mem | Admin/Mem | Admin/Mem | Admin/Mem | Admin/Mem | Group chats only |
| `/conversations/{id}/members/{u}`| DELETE | Authenticated | Self/Admin| Self/Admin| Self/Admin| Self/Admin| Self/Admin| Group admin or leaving self |
| **Messages** | | | | | | | | |
| `/conversations/{id}/messages`| GET | Authenticated | Member | Member | Member | Member | Member | Member access check |
| `/conversations/{id}/messages`| POST | `SEND_MESSAGE` | Member | Member | Member | Member | Member | Post message in chat |
| `/channels/{id}/messages` | GET | Authenticated | Channel | Channel | Channel | Channel | Channel | Evaluates channel type |
| `/channels/{id}/messages` | POST | `SEND_MESSAGE` | Channel*| Channel*| Channel*| Channel*| Channel*| *Blocked on read-only |
| `/messages/{id}` | PATCH | `SEND_MESSAGE` | Author | Author | Author | Author | Author | Sender only |
| `/messages/{id}` | DELETE | `DELETE_MESSAGE` | Author | Author | Mod/Author| Mod/Author| Yes | Author or channel moderator |
| **Channels** | | | | | | | | |
| `/channels` | GET | Authenticated | Yes | Yes | Yes | Yes | Yes | Lists accessible channels |
| `/channels/{id}` | GET | Authenticated | Scope* | Scope* | Scope* | Scope* | Scope* | *Dynamic hierarchy check |
| `/channels` | POST | `CREATE_CHANNEL` | No | Team* | Dept* | Yes | Yes | *Scoped to team/dept |
| `/channels/{id}` | PATCH | `MANAGE_CHANNEL` | No | Lead* | Mgr* | Yes | Yes | Channel admin/creator |
| `/channels/{id}` | DELETE | `MANAGE_CHANNEL` | No | No | Mgr* | Yes | Yes | Archive / soft delete |
| `/channels/{id}/members` | GET/POST | Authenticated | Private* | Private* | Private* | Private* | Private* | *Private channel member |
| **Announcements** | | | | | | | | |
| `/announcements` | GET | Authenticated | Target | Target | Target | Target | Target | Filtered by audience scope (active/published) |
| `/announcements/{id}` | GET | Authenticated | Target | Target | Target | Target | Target | View details (records read timestamp) |
| `/announcements` | POST | `CREATE_ANNOUNCEMENT`| No | Team* | Dept* | Yes | Yes | *Scope restricted (Team Lead/Dept Manager) |
| `/announcements/{id}` | PUT | `EDIT_ANNOUNCEMENT` | No | Author | Author | Yes | Yes | Author or HR/Super Admin (DRAFT/SCHEDULED only) |
| `/announcements/{id}/publish`| POST | `PUBLISH_ANNOUNCEMENTS`| No | Author* | Author* | Yes | Yes | Transitions to PUBLISHED state with audit event |
| `/announcements/{id}/schedule`| POST | `SCHEDULE_ANNOUNCEMENTS`| No | Author* | Author* | Yes | Yes | Sets future publication schedule |
| `/announcements/{id}/cancel` | POST | `CANCEL_ANNOUNCEMENTS`| No | Author* | Author* | Yes | Yes | Safe cancellation without deletion |
| `/announcements/{id}/archive`| POST | `ARCHIVE_ANNOUNCEMENTS`| No | Author* | Author* | Yes | Yes | Moves published notice to ARCHIVED |
| `/announcements/{id}/read` | POST | Authenticated | Target | Target | Target | Target | Target | Explicit non-destructive read tracking |
| `/announcements/{id}/acknowledge`| POST | `ACKNOWLEDGE_ANNOUNCEMENTS`| Target | Target | Target | Target | Target | Formal sign-off for mandatory notices |
| `/announcements/{id}/acknowledgements` | GET | Authenticated | No | Lead* | Mgr* | Yes | Yes | Compliance report (scoped to author/lead/mgr/admin) |
| **Documents** | | | | | | | | |
| `/documents` | GET | `VIEW_DOCUMENTS` | Scope | Scope | Scope | Scope | Scope | Visibility scope check |
| `/documents/{id}` | GET | `VIEW_DOCUMENTS` | Scope | Scope | Scope | Scope | Scope | Generates pre-signed download |
| `/documents/upload-intent` | POST | `MANAGE_DOCUMENTS` | No | Lead | Mgr | Yes | Yes | Generates pre-signed PUT |
| `/documents/{id}/complete-upload`| POST| `MANAGE_DOCUMENTS` | No | Lead | Mgr | Yes | Yes | Validates S3 upload |
| `/documents/{id}` | PATCH/DELETE| `MANAGE_DOCUMENTS` | No | Author | Author | Yes | Yes | Uploader or Admin |
| **Meetings & Calendar** | | | | | | | | |
| `/meetings` | GET | Authenticated | Invitee | Invitee | Invitee | Invitee | Invitee | Organizer or participant |
| `/meetings/{id}` | GET | Authenticated | Invitee | Invitee | Invitee | Invitee | Invitee | Meeting details |
| `/meetings` | POST | `CREATE_MEETING` | Yes | Yes | Yes | Yes | Yes | Shift sync / team meeting |
| `/meetings/{id}` | PATCH/DELETE| `MANAGE_MEETING` | Org | Org | Org/Mgr | Org/HR | Yes | Organizer or Admin |
| `/meetings/{id}/participants/*`| POST/DEL | `MANAGE_MEETING` | Org | Org | Org/Mgr | Org/HR | Yes | Organizer only |
| `/meetings/{id}/participants/{u}/response`| PATCH | Authenticated | Self | Self | Self | Self | Self | Update own RSVP |
| `/calendar/events` | GET | Authenticated | Yes | Yes | Yes | Yes | Yes | Personal + team schedule |
| **Notifications** | | | | | | | | |
| `/notifications` | GET | Authenticated | Self | Self | Self | Self | Self | Own paginated notification feed |
| `/notifications/unread-count` | GET | Authenticated | Self | Self | Self | Self | Self | Total unread notification counter |
| `/notifications/{id}/read` | POST | Authenticated | Self | Self | Self | Self | Self | Mark single read (IDOR scoped) |
| `/notifications/read-all` | POST | Authenticated | Self | Self | Self | Self | Self | Mark all read for current user |
| **Search** | | | | | | | | |
| `/search` | GET | Authenticated | Filtered| Filtered| Filtered| Filtered| Filtered| Scoped to user's permissions |
| **Admin Cockpit** | | | | | | | | |
| `/admin/dashboard` | GET | `MANAGE_ROLES` | No | No | No | Limited | Yes | Executive telemetry |
| `/admin/system/health` | GET | `SUPER_ADMIN` | No | No | No | No | Yes | Infrastructure health |
| **Audit Logs** | | | | | | | | |
| `/admin/audit-logs` | GET | `VIEW_AUDIT_LOGS` | No | No | No | Emp Scope| Yes | Immutable compliance trail |

---

## 4. Dynamic Organizational Authorization Rules

### 4.1 Channel Access Evaluation Logic

When an employee requests access to `/api/v1/channels/{id}` or sends a message:
1. If `channel.type == 'COMPANY'`: Grant access to all active employees.
2. If `channel.type == 'DEPARTMENT'`: Grant access if `current_user.employee.department_id == channel.department_id` or user holds `SUPER_ADMIN`.
3. If `channel.type == 'TEAM'`: Grant access if `current_user.employee.team_id == channel.team_id` or user holds `SUPER_ADMIN`.
4. If `channel.type == 'PRIVATE'`: Query `channel_members` for `(channel_id, current_user.id)`. Grant access only if record exists.

### 4.2 Announcement Scope Evaluation Logic

When an announcement is published:
1. `audience_type == 'ALL'` or `'ALL_EMPLOYEES'`: Fan-out notifications to entire active employee directory.
2. `audience_type == 'DEPARTMENT'`: Visible only to employees where `employee.department_id == announcement.department_id`.
3. `audience_type == 'TEAM'`: Visible only to employees where `employee.team_id == announcement.team_id`.
4. `audience_type == 'ROLE'`: Visible only to users assigned `target_role_id`.

### 4.3 Direct Conversation Privacy Guarantee

When user A queries `/api/v1/conversations/{id}`:
- Check `conversation_members` where `conversation_id = id AND user_id = current_user.id AND left_at IS NULL`.
- If no record exists, reject with `403 FORBIDDEN` (or `404 NOT FOUND` to prevent ID enumeration).
