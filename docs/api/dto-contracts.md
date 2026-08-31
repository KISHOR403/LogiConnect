# LogiConnect REST API - DTO Contracts Specification

## 1. Overview & Data Transfer Object (DTO) Directives

This document defines the comprehensive Request and Response DTO specifications for the **LogiConnect** platform across all 18 domains.

### Core DTO Guidelines

1. **Entity Encapsulation**: Database entities (JPA entities) must never be returned or received directly by any API controller.
2. **Credential Protection**: Passwords, password hashes (`password_hash`), lockout tokens, and internal salt values are strictly prohibited from all response DTOs.
3. **Bean Validation Standards**: Every incoming request DTO specifies Jakarta/Hibernate validation constraints (`@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Pattern`, `@Positive`, `@PastOrPresent`).
4. **JSON Property Naming**: All fields use standard `camelCase`.
5. **Date & Time Serialization**: All timestamps are formatted according to ISO-8601 UTC standard (`YYYY-MM-DD'T'HH:mm:ss'Z'`), e.g., `2026-08-31T12:00:00Z`. Dates use `YYYY-MM-DD`.
6. **Identifiers**: All IDs are formatted as standard UUIDv4 strings (e.g. `e0000000-0000-0000-0000-000000000001`).

---

## 2. Shared & Envelope DTOs

### 2.1 `StandardResponse<T>`
Universal envelope for non-paginated API responses.

| Field | Type | Description |
| :--- | :--- | :--- |
| `success` | `boolean` | `true` if operation completed successfully |
| `data` | `T` (Generic) | Response payload object or array |
| `message` | `string` | Human-readable outcome description |

### 2.2 `PageResponse<T>`
Offset-based pagination envelope for directory and collection queries.

| Field | Type | Description |
| :--- | :--- | :--- |
| `items` | `List<T>` | List of elements for the requested page |
| `pagination` | `PaginationMetadata` | Pagination details |

#### `PaginationMetadata`
| Field | Type | Description |
| :--- | :--- | :--- |
| `page` | `integer` | 0-indexed current page number |
| `size` | `integer` | Number of items per page |
| `totalElements` | `long` | Total items matching query across all pages |
| `totalPages` | `integer` | Total number of available pages |
| `first` | `boolean` | `true` if this is the first page |
| `last` | `boolean` | `true` if this is the last page |
| `hasNext` | `boolean` | `true` if subsequent pages exist |
| `hasPrevious` | `boolean` | `true` if preceding pages exist |

### 2.3 `CursorResponse<T>`
Cursor-based pagination envelope for high-volume message and notification feeds.

| Field | Type | Description |
| :--- | :--- | :--- |
| `items` | `List<T>` | Chronological items |
| `cursor` | `CursorMetadata` | Cursor pagination details |

#### `CursorMetadata`
| Field | Type | Description |
| :--- | :--- | :--- |
| `nextCursor` | `string` (nullable) | Base64-encoded pointer for the next page |
| `prevCursor` | `string` (nullable) | Base64-encoded pointer for the previous page |
| `hasMore` | `boolean` | `true` if more records exist in current direction |
| `limit` | `integer` | Page limit requested |

### 2.4 `ErrorResponse`
Standard error envelope returned for all 4xx and 5xx responses.

| Field | Type | Description |
| :--- | :--- | :--- |
| `success` | `boolean` | Always `false` |
| `error` | `ErrorDetail` | Error classification, message, and field errors |
| `timestamp` | `string` (ISO-8601) | Timestamp when error occurred |
| `path` | `string` | Target request URI path |

#### `ErrorDetail`
| Field | Type | Description |
| :--- | :--- | :--- |
| `code` | `string` | Machine-readable error code (e.g. `VALIDATION_FAILED`) |
| `message` | `string` | User-friendly explanation of error |
| `details` | `List<FieldErrorDetail>` | Array of field-level validation errors |

#### `FieldErrorDetail`
| Field | Type | Description |
| :--- | :--- | :--- |
| `field` | `string` | JSON request field path |
| `rejectedValue` | `any` | Value provided in request |
| `issue` | `string` | Specific rule violation description |

---

## 3. Domain DTO Contracts

---

### Domain 1: Authentication DTOs

#### `LoginRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `employeeCode` | `string` | Yes | `@NotBlank`, max 255. Accepts either Employee Code (e.g. `EMP10001`) or official corporate email (e.g. `priya.sharma@logiconnect.internal`). |
| `password` | `string` | Yes | `@NotBlank`, max 128 |
| `deviceInfo` | `string` | No | Optional client/device name (e.g. `"Chrome 128 on macOS"`) |

*Example Request:*
```json
{
  "employeeCode": "EMP10001",
  "password": "SecurePassword@123",
  "deviceInfo": "Chrome 128 on Windows 11"
}
```

#### `RefreshTokenRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `refreshToken` | `string` | Yes | `@NotBlank` |

#### `AuthTokenResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `accessToken` | `string` | Signed JWT bearer token (15 min validity) |
| `refreshToken` | `string` | Cryptographic refresh token (7 days validity) |
| `tokenType` | `string` | Always `"Bearer"` |
| `expiresIn` | `long` | Access token lifespan in seconds (e.g. 900) |
| `user` | `CurrentUserResponse` | Authenticated user profile summary |

*Example Response:*
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "4c9d968b-592f-4e0e-8fcb-21a4fa857092",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "f0000000-0000-0000-0000-000000000002",
    "employeeId": "e0000000-0000-0000-0000-000000000002",
    "employeeCode": "EMP-1002",
    "firstName": "Priya",
    "lastName": "Sharma",
    "email": "priya.sharma@logiconnect.internal",
    "designation": "HR Director",
    "departmentName": "Human Resources",
    "roles": ["HR_ADMIN"],
    "permissions": ["MANAGE_EMPLOYEES", "VIEW_EMPLOYEES", "CREATE_ANNOUNCEMENT"]
  }
}
```

#### `ChangePasswordRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `currentPassword` | `string` | Yes | `@NotBlank` |
| `newPassword` | `string` | Yes | `@NotBlank`, `@Size(min=8, max=100)`, `@Pattern` (must contain uppercase, lowercase, digit, special character) |

#### `ForgotPasswordRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `email` | `string` | Yes | `@NotBlank`, `@Email` |

#### `ResetPasswordRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `token` | `string` | Yes | `@NotBlank` |
| `newPassword` | `string` | Yes | `@NotBlank`, `@Size(min=8, max=100)` |

---

### Domain 2: Employee DTOs

#### `CreateEmployeeRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `employeeCode` | `string` | Yes | `@NotBlank`, `@Size(max=50)`, Pattern: `^EMP-[0-9]{4,8}$` |
| `firstName` | `string` | Yes | `@NotBlank`, `@Size(max=100)` |
| `lastName` | `string` | Yes | `@NotBlank`, `@Size(max=100)` |
| `email` | `string` | Yes | `@NotBlank`, `@Email`, `@Size(max=255)` |
| `phone` | `string` | No | `@Pattern(regexp = "^\\+?[0-9]{10,15}$")` |
| `designation` | `string` | Yes | `@NotBlank`, `@Size(max=100)` |
| `departmentId` | `string` (UUID) | Yes | `@NotNull` |
| `teamId` | `string` (UUID) | No | Optional team assignment |
| `managerId` | `string` (UUID) | No | Optional supervisor UUID |
| `location` | `string` | Yes | `@NotBlank`, `@Size(max=100)` |
| `joiningDate` | `string` (Date) | Yes | `@NotNull`, Format: `YYYY-MM-DD` |

*Example Request:*
```json
{
  "employeeCode": "EMP-1006",
  "firstName": "Kavita",
  "lastName": "Nair",
  "email": "kavita.nair@logiconnect.internal",
  "phone": "+919876543215",
  "designation": "Dispatch Lead",
  "departmentId": "c0000000-0000-0000-0000-000000000003",
  "teamId": "d0000000-0000-0000-0000-000000000001",
  "managerId": "e0000000-0000-0000-0000-000000000003",
  "location": "Bangalore Hub",
  "joiningDate": "2026-09-01"
}
```

#### `UpdateEmployeeRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `firstName` | `string` | No | `@Size(max=100)` |
| `lastName` | `string` | No | `@Size(max=100)` |
| `phone` | `string` | No | Phone regex pattern |
| `profilePhotoUrl` | `string` | No | Valid URL |
| `designation` | `string` | No | `@Size(max=100)` |
| `departmentId` | `string` (UUID) | No | Valid UUID |
| `teamId` | `string` (UUID) | No | Valid UUID |
| `managerId` | `string` (UUID) | No | Valid UUID |
| `location` | `string` | No | `@Size(max=100)` |

#### `UpdateEmployeeStatusRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `status` | `string` | Yes | Enum: `ACTIVE`, `PROBATION`, `ON_LEAVE`, `SUSPENDED`, `TERMINATED`, `RESIGNED` |
| `exitDate` | `string` (Date) | No | Mandatory if `status` is `TERMINATED` or `RESIGNED` |
| `reason` | `string` | No | `@Size(max=500)` |

#### `EmployeeSummaryResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Employee unique identifier |
| `employeeCode` | `string` | Badge/Employee code (e.g. `EMP-1003`) |
| `fullName` | `string` | Computed full name |
| `email` | `string` | Corporate email |
| `phone` | `string` | Phone contact |
| `profilePhotoUrl` | `string` | CDN/S3 image URL |
| `designation` | `string` | Role title |
| `departmentName` | `string` | Assigned department |
| `teamName` | `string` | Assigned team |
| `location` | `string` | Logistics hub or facility location |
| `status` | `string` | Employment status |

#### `EmployeeDetailResponse`
Includes all `EmployeeSummaryResponse` fields plus:
| Field | Type | Description |
| :--- | :--- | :--- |
| `departmentId` | `string` (UUID) | Department UUID |
| `teamId` | `string` (UUID) | Team UUID |
| `managerId` | `string` (UUID) | Manager UUID |
| `managerName` | `string` | Manager full name |
| `joiningDate` | `string` (Date) | Start date (`YYYY-MM-DD`) |
| `exitDate` | `string` (Date) | Offboarding date (if applicable) |
| `createdAt` | `string` (ISO-8601) | Registration timestamp |
| `updatedAt` | `string` (ISO-8601) | Last modified timestamp |

---

### Domain 3: Department & Team DTOs

#### `CreateDepartmentRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `code` | `string` | Yes | `@NotBlank`, Pattern: `^DEPT-[A-Z0-9_-]{2,20}$` |
| `name` | `string` | Yes | `@NotBlank`, `@Size(max=100)` |
| `description` | `string` | No | `@Size(max=1000)` |
| `managerId` | `string` (UUID) | No | Target employee UUID |

#### `DepartmentDetailResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Department UUID |
| `code` | `string` | Department code |
| `name` | `string` | Department title |
| `description` | `string` | Scope description |
| `managerId` | `string` (UUID) | Head of department UUID |
| `managerName` | `string` | Head of department full name |
| `status` | `string` | `ACTIVE`, `INACTIVE`, `ARCHIVED` |
| `teamCount` | `integer` | Number of operational teams |
| `employeeCount` | `integer` | Active headcount |
| `createdAt` | `string` (ISO-8601) | Creation timestamp |

#### `CreateTeamRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `departmentId` | `string` (UUID) | Yes | `@NotNull` |
| `code` | `string` | Yes | `@NotBlank`, Pattern: `^TEAM-[A-Z0-9_-]{2,20}$` |
| `name` | `string` | Yes | `@NotBlank`, `@Size(max=100)` |
| `description` | `string` | No | `@Size(max=1000)` |
| `teamLeadId` | `string` (UUID) | No | Lead employee UUID |

#### `TeamDetailResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Team UUID |
| `departmentId` | `string` (UUID) | Parent department UUID |
| `departmentName` | `string` | Parent department name |
| `code` | `string` | Team code |
| `name` | `string` | Team name |
| `description` | `string` | Operational focus |
| `teamLeadId` | `string` (UUID) | Team lead UUID |
| `teamLeadName` | `string` | Team lead full name |
| `status` | `string` | Status code |
| `memberCount` | `integer` | Total assigned employees |

---

### Domain 4: Conversation DTOs

#### `CreateDirectConversationRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `targetUserId` | `string` (UUID) | Yes | `@NotNull` (Must not equal `current_user.id`) |

#### `CreateGroupConversationRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `name` | `string` | Yes | `@NotBlank`, `@Size(min=2, max=150)` |
| `description` | `string` | No | `@Size(max=500)` |
| `avatarUrl` | `string` | No | Valid image URL |
| `memberUserIds` | `List<string>` | Yes | `@NotEmpty`, `@Size(min=1, max=100)` |

#### `ConversationSummaryResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Conversation UUID |
| `type` | `string` | `DIRECT` or `GROUP` |
| `name` | `string` | Title or direct partner's full name |
| `avatarUrl` | `string` | Conversation or participant photo URL |
| `lastMessage` | `MessageSnippetResponse` | Text and timestamp of most recent message |
| `unreadCount` | `integer` | Unread messages for current user |
| `isMuted` | `boolean` | Notification mute state |
| `isPinned` | `boolean` | Pinned to top of chat list |
| `lastMessageAt` | `string` (ISO-8601) | Timestamp of latest activity |

#### `ConversationDetailResponse`
Includes all summary fields plus:
| Field | Type | Description |
| :--- | :--- | :--- |
| `description` | `string` | Group topic/description |
| `createdBy` | `string` (UUID) | Creator user UUID |
| `members` | `List<ConversationMemberResponse>` | List of active participants |
| `createdAt` | `string` (ISO-8601) | Room creation timestamp |

#### `ConversationMemberResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `userId` | `string` (UUID) | Member user UUID |
| `employeeId` | `string` (UUID) | Member employee UUID |
| `fullName` | `string` | Member display name |
| `profilePhotoUrl` | `string` | Avatar URL |
| `role` | `string` | `MEMBER` or `ADMIN` |
| `joinedAt` | `string` (ISO-8601) | Membership start timestamp |
| `isOnline` | `boolean` | Real-time presence flag |

---

### Domain 5: Message DTOs

#### `SendMessageRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `messageType` | `string` | Yes | Enum: `TEXT`, `FILE`, `IMAGE`, `SYSTEM`, `LOCATION`, `AUDIO`, `VIDEO` |
| `content` | `string` | Conditional | Required for `TEXT`, max 10,000 characters |
| `replyToMessageId` | `string` (UUID) | No | For threaded replies |
| `attachments` | `List<MessageAttachmentDto>` | No | Required if `messageType` is `FILE`, `IMAGE`, `AUDIO`, `VIDEO` |

#### `MessageAttachmentDto`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `storageKey` | `string` | Yes | `@NotBlank` |
| `fileUrl` | `string` | Yes | `@NotBlank` |
| `fileName` | `string` | Yes | `@NotBlank` |
| `fileType` | `string` | Yes | MIME type |
| `fileSize` | `long` | Yes | `@Positive` |

#### `EditMessageRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `content` | `string` | Yes | `@NotBlank`, `@Size(max=10000)` |

#### `MessageResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Unique message identifier |
| `conversationId` | `string` (UUID) | Set if direct/group chat (mutually exclusive with `channelId`) |
| `channelId` | `string` (UUID) | Set if channel message |
| `sender` | `MessageSenderResponse` | Sender user details (id, name, avatar, designation) |
| `messageType` | `string` | `TEXT`, `FILE`, `IMAGE`, etc. |
| `content` | `string` | Message text (or `"[Message deleted]"` if soft-deleted) |
| `replyTo` | `MessageSnippetResponse` | Parent message snippet if reply |
| `attachments` | `List<MessageAttachmentResponse>` | Uploaded file references |
| `isPinned` | `boolean` | Pinned status |
| `isEdited` | `boolean` | `true` if `editedAt` is populated |
| `isDeleted` | `boolean` | `true` if `deletedAt` is populated |
| `createdAt` | `string` (ISO-8601) | Sending timestamp |
| `editedAt` | `string` (ISO-8601) | Modification timestamp |

*Example Message Response:*
```json
{
  "id": "70000000-0000-0000-0000-000000000001",
  "channelId": "10000000-0000-0000-0000-000000000003",
  "conversationId": null,
  "sender": {
    "userId": "f0000000-0000-0000-0000-000000000003",
    "fullName": "Rajesh Kumar",
    "designation": "General Manager Operations",
    "profilePhotoUrl": "https://cdn.logiconnect.internal/avatars/rajesh.jpg"
  },
  "messageType": "TEXT",
  "content": "All line-haul trucks for Bangalore-Chennai corridor have departed on schedule.",
  "replyTo": null,
  "attachments": [],
  "isPinned": false,
  "isEdited": false,
  "isDeleted": false,
  "createdAt": "2026-08-31T06:30:00Z",
  "editedAt": null
}
```

---

### Domain 6: Channel DTOs

#### `CreateChannelRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `name` | `string` | Yes | `@NotBlank`, `@Size(min=2, max=100)` |
| `slug` | `string` | Yes | `@NotBlank`, Pattern: `^[a-z0-9-]+$` |
| `description` | `string` | No | `@Size(max=1000)` |
| `type` | `string` | Yes | Enum: `COMPANY`, `DEPARTMENT`, `TEAM`, `PRIVATE` |
| `departmentId` | `string` (UUID) | Conditional | Mandatory if `type == DEPARTMENT` |
| `teamId` | `string` (UUID) | Conditional | Mandatory if `type == TEAM` |
| `isReadOnly` | `boolean` | No | Default `false` |

#### `ChannelDetailResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Channel identifier |
| `name` | `string` | Display name (e.g. `#operations-dispatch`) |
| `slug` | `string` | URL slug |
| `description` | `string` | Scope and guidelines |
| `type` | `string` | `COMPANY`, `DEPARTMENT`, `TEAM`, `PRIVATE` |
| `departmentId` | `string` (UUID) | Associated department UUID |
| `departmentName` | `string` | Associated department name |
| `teamId` | `string` (UUID) | Associated team UUID |
| `teamName` | `string` | Associated team name |
| `isReadOnly` | `boolean` | Broadcast channel restriction |
| `memberCount` | `integer` | Active participant count |
| `unreadCount` | `integer` | Unread messages for current user |
| `status` | `string` | `ACTIVE`, `ARCHIVED`, `DELETED` |
| `createdAt` | `string` (ISO-8601) | Creation timestamp |

---

### Domain 7: Announcement DTOs

#### `CreateAnnouncementRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `title` | `string` | Yes | `@NotBlank`, `@Size(max=255)` |
| `content` | `string` | Yes | `@NotBlank`, Markdown/Text body |
| `priority` | `string` | Yes | Enum: `NORMAL`, `IMPORTANT`, `URGENT`, `EMERGENCY` |
| `audienceType` | `string` | Yes | Enum: `ALL`, `DEPARTMENT`, `TEAM`, `ROLE`, `INDIVIDUAL`, `ALL_EMPLOYEES` |
| `departmentId` | `string` (UUID) | Conditional | Mandatory if `audienceType == DEPARTMENT` |
| `teamId` | `string` (UUID) | Conditional | Mandatory if `audienceType == TEAM` |
| `targetRoleId` | `string` (UUID) | Conditional | Mandatory if `audienceType == ROLE` |
| `targetLocation` | `string` | No | Optional location targeting |
| `requiresAcknowledgement`| `boolean`| No | Default `false` |
| `expiresAt` | `string` (ISO-8601) | No | Optional auto-archive cutoff |

*Example Request:*
```json
{
  "title": "Severe Weather Alert: Heavy Rainfall at Bangalore Hub",
  "content": "Due to torrential rains, loading dock bays 4-8 are temporarily closed. All driver dispatches are routed through North Gate. Safety gear is mandatory.",
  "priority": "URGENT",
  "audienceType": "DEPARTMENT",
  "departmentId": "c0000000-0000-0000-0000-000000000003",
  "requiresAcknowledgement": true,
  "expiresAt": "2026-09-02T23:59:59Z"
}
```

#### `AnnouncementDetailResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Announcement UUID |
| `title` | `string` | Headline |
| `content` | `string` | Full announcement text |
| `author` | `AuthorSummaryResponse` | Author user details |
| `priority` | `string` | `NORMAL`, `IMPORTANT`, `URGENT`, `EMERGENCY` |
| `audienceType` | `string` | Audience classification |
| `targetScope` | `string` | Description of target group (e.g. "Department: Operations") |
| `publishedAt` | `string` (ISO-8601) | Publication timestamp |
| `expiresAt` | `string` (ISO-8601) | Expiration timestamp |
| `requiresAcknowledgement`| `boolean` | Mandatory sign-off requirement |
| `isRead` | `boolean` | Current user read state |
| `isAcknowledged` | `boolean` | Current user acknowledgement state |
| `acknowledgedAt` | `string` (ISO-8601) | When current user acknowledged |
| `status` | `string` | `DRAFT`, `PUBLISHED`, `ARCHIVED`, `CANCELLED` |

#### `AnnouncementStatisticsResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `announcementId` | `string` (UUID) | Notice UUID |
| `totalTargeted` | `integer` | Total eligible employees in audience |
| `readCount` | `integer` | Number of employees who viewed notice |
| `acknowledgedCount` | `integer` | Number of employees who signed acknowledgement |
| `acknowledgedPercentage` | `double` | Percentage compliant (0.0 to 100.0) |
| `unacknowledgedEmployees`| `List<EmployeeSummaryResponse>` | List of employees pending acknowledgement |

---

### Domain 8: Document Management DTOs

#### `DocumentUploadIntentRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `name` | `string` | Yes | `@NotBlank`, `@Size(max=255)` |
| `description` | `string` | No | `@Size(max=1000)` |
| `fileName` | `string` | Yes | `@NotBlank` (e.g. `sop_loading_v2.pdf`) |
| `fileType` | `string` | Yes | `@NotBlank` MIME type (e.g. `application/pdf`) |
| `fileSize` | `long` | Yes | `@Positive`, max 104,857,600 (100MB) |
| `checksum` | `string` | Yes | `@Pattern` (64-char SHA-256 hex string) |
| `visibility` | `string` | Yes | Enum: `PUBLIC`, `INTERNAL`, `DEPARTMENT`, `TEAM`, `CONFIDENTIAL` |
| `departmentId` | `string` (UUID) | No | Associated department |
| `teamId` | `string` (UUID) | No | Associated team |

*Example Request:*
```json
{
  "name": "Standard Operating Procedure: Hazardous Material Line-Haul",
  "description": "Compliance SOP for chemical transport corridors",
  "fileName": "Hazmat_SOP_2026.pdf",
  "fileType": "application/pdf",
  "fileSize": 4194304,
  "checksum": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "visibility": "DEPARTMENT",
  "departmentId": "c0000000-0000-0000-0000-000000000003"
}
```

#### `DocumentUploadIntentResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `documentId` | `string` (UUID) | Initialized document record UUID in `PENDING` state |
| `storageKey` | `string` | S3/MinIO destination object key |
| `uploadUrl` | `string` | Pre-Signed S3/MinIO HTTP PUT URL |
| `requiredHeaders`| `Map<string, string>` | Headers client must pass to S3 PUT (e.g. Content-Type) |
| `expiresAt` | `string` (ISO-8601) | Expiration time of pre-signed URL (15 minutes) |

#### `DocumentDetailResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Document UUID |
| `name` | `string` | Document title |
| `description` | `string` | Description |
| `fileName` | `string` | Original file name |
| `fileType` | `string` | MIME type |
| `fileSize` | `long` | Size in bytes |
| `downloadUrl` | `string` | Pre-signed download URL (active for 15 min) |
| `visibility` | `string` | Visibility scope |
| `uploadedBy` | `AuthorSummaryResponse` | Uploader profile summary |
| `departmentName`| `string` | Associated department |
| `status` | `string` | `ACTIVE`, `ARCHIVED`, `DELETED` |
| `createdAt` | `string` (ISO-8601) | Upload timestamp |

---

### Domain 9: Meeting & Calendar DTOs

#### `CreateMeetingRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `title` | `string` | Yes | `@NotBlank`, `@Size(max=255)` |
| `description` | `string` | No | `@Size(max=2000)` |
| `startTime` | `string` (ISO-8601) | Yes | `@NotNull`, `@FutureOrPresent` |
| `endTime` | `string` (ISO-8601) | Yes | `@NotNull`, Must be after `startTime` |
| `location` | `string` | No | Room or Hub name |
| `meetingLink` | `string` | No | Video URL (e.g. Jitsi/Zoom/Teams) |
| `departmentId` | `string` (UUID) | No | Optional department link |
| `teamId` | `string` (UUID) | No | Optional team link |
| `participantUserIds`| `List<string>` | Yes | `@NotEmpty` |

#### `MeetingDetailResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Meeting UUID |
| `title` | `string` | Title |
| `description` | `string` | Agenda |
| `organizer` | `AuthorSummaryResponse` | Organizer details |
| `startTime` | `string` (ISO-8601) | Start time |
| `endTime` | `string` (ISO-8601) | End time |
| `location` | `string` | Facility / Room |
| `meetingLink` | `string` | Video conference link |
| `status` | `string` | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `participants` | `List<MeetingParticipantResponse>` | List of invited participants with RSVP status |

#### `MeetingParticipantResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `userId` | `string` (UUID) | Invitee user UUID |
| `fullName` | `string` | Invitee full name |
| `designation` | `string` | Invitee job title |
| `response` | `string` | `PENDING`, `ACCEPTED`, `DECLINED`, `TENTATIVE` |
| `joinedAt` | `string` (ISO-8601) | When participant joined conference (if tracked) |

#### `MeetingRsvpRequest`
| Field | Type | Required | Constraints |
| :--- | :--- | :--- | :--- |
| `response` | `string` | Yes | Enum: `ACCEPTED`, `DECLINED`, `TENTATIVE` |

---

### Domain 10: Notification DTOs

#### `NotificationResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Notification identifier |
| `type` | `string` | `ANNOUNCEMENT`, `MESSAGE`, `MEETING_INVITE`, `MENTION`, `SYSTEM` |
| `title` | `string` | Short subject |
| `message` | `string` | Notification body |
| `referenceType` | `string` | E.g. `ANNOUNCEMENT`, `CONVERSATION`, `CHANNEL`, `MEETING` |
| `referenceId` | `string` (UUID) | Target entity ID for in-app navigation deep linking |
| `isRead` | `boolean` | Read flag |
| `readAt` | `string` (ISO-8601) | When marked read |
| `createdAt` | `string` (ISO-8601) | Delivery timestamp |

---

### Domain 11: Audit Log DTOs

#### `AuditLogResponse`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` (UUID) | Immutable audit log record UUID |
| `actor` | `AuditActorResponse` | User who triggered the action (id, name, email) |
| `action` | `string` | Action code (e.g. `USER_LOGIN`, `ANNOUNCEMENT_ACKNOWLEDGED`) |
| `entityType` | `string` | `USER`, `EMPLOYEE`, `ANNOUNCEMENT`, `DOCUMENT`, `ROLE` |
| `entityId` | `string` (UUID) | Modified target entity UUID |
| `ipAddress` | `string` | Client IP (IPv4 or IPv6) |
| `userAgent` | `string` | Client user agent string |
| `metadata` | `Map<string, Object>` | Contextual change snapshot (before/after parameters) |
| `createdAt` | `string` (ISO-8601) | Exact timestamp of event occurrence |
