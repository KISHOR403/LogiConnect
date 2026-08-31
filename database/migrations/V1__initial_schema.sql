-- ============================================================================
-- LogiConnect Platform - PostgreSQL Initial Database Schema
-- Migration: V1__initial_schema.sql
-- Description: Core schema for logistics enterprise collaboration platform (2,000+ employees)
-- Compatible with: PostgreSQL 14+, Spring Boot 3+, Spring Data JPA, Hibernate
-- ============================================================================

-- Enable UUID extension (PostgreSQL pgcrypto)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. UTILITY FUNCTIONS & TRIGGERS
-- ============================================================================

-- Auto-update 'updated_at' column timestamp trigger function
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 2. ORGANIZATIONAL STRUCTURE DOMAIN (Departments, Teams, Employees)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: departments
-- Purpose: Top-level organizational departments (e.g., Operations, Warehouse, HR)
-- ----------------------------------------------------------------------------
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    manager_id UUID, -- Foreign key to employees(id) added via ALTER TABLE
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Table: teams
-- Purpose: Functional units within departments (e.g., Bangalore Ops, Mumbai Support)
-- ----------------------------------------------------------------------------
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    team_lead_id UUID, -- Foreign key to employees(id) added via ALTER TABLE
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_teams_dept_name UNIQUE (department_id, name)
);

-- ----------------------------------------------------------------------------
-- Table: employees
-- Purpose: Core employee profile and directory details
-- ----------------------------------------------------------------------------
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(30),
    profile_photo_url VARCHAR(1000),
    designation VARCHAR(100) NOT NULL,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    manager_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    location VARCHAR(100) NOT NULL,
    joining_date DATE NOT NULL,
    exit_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PROBATION', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED', 'RESIGNED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add deferred circular foreign keys for department managers and team leads
ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL;

ALTER TABLE teams
    ADD CONSTRAINT fk_teams_team_lead FOREIGN KEY (team_lead_id) REFERENCES employees(id) ON DELETE SET NULL;

-- ============================================================================
-- 3. AUTHENTICATION & ACCESS CONTROL DOMAIN (Users, Roles, Permissions)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: users
-- Purpose: Authentication credentials, login state, and security flags
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL UNIQUE REFERENCES employees(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_VERIFICATION')),
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Table: roles
-- Purpose: System and business security roles (e.g., SUPER_ADMIN, MANAGER, EMPLOYEE)
-- ----------------------------------------------------------------------------
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Table: permissions
-- Purpose: Granular operation permissions for RBAC enforcement
-- ----------------------------------------------------------------------------
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    module VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Table: user_roles
-- Purpose: Many-to-many relationship linking users to roles
-- ----------------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (user_id, role_id)
);

-- ----------------------------------------------------------------------------
-- Table: role_permissions
-- Purpose: Many-to-many relationship linking roles to permissions
-- ----------------------------------------------------------------------------
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================================
-- 4. COMMUNICATION & MESSAGING DOMAIN (Conversations, Channels, Messages)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: conversations
-- Purpose: Direct (1:1) and ad-hoc multi-party group chat rooms
-- ----------------------------------------------------------------------------
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(20) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name VARCHAR(150),
    description TEXT,
    avatar_url VARCHAR(1000),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    last_message_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Table: conversation_members
-- Purpose: Participants belonging to a conversation room
-- ----------------------------------------------------------------------------
CREATE TABLE conversation_members (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('MEMBER', 'ADMIN')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ,
    last_read_at TIMESTAMPTZ,
    is_muted BOOLEAN NOT NULL DEFAULT FALSE,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (conversation_id, user_id)
);

-- ----------------------------------------------------------------------------
-- Table: channels
-- Purpose: Topic-based, department-based, or team-based organizational channels
-- ----------------------------------------------------------------------------
CREATE TABLE channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('COMPANY', 'DEPARTMENT', 'TEAM', 'PRIVATE')),
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    is_read_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Table: channel_members
-- Purpose: Membership records for private and specific targeted channels
-- ----------------------------------------------------------------------------
CREATE TABLE channel_members (
    channel_id UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('MEMBER', 'MODERATOR', 'ADMIN')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_read_at TIMESTAMPTZ,
    is_muted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (channel_id, user_id)
);

-- ----------------------------------------------------------------------------
-- Table: messages
-- Purpose: Chat and broadcast messages in conversations or channels
-- ----------------------------------------------------------------------------
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    channel_id UUID REFERENCES channels(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES users(id) ON DELETE SET NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'FILE', 'IMAGE', 'SYSTEM', 'LOCATION', 'AUDIO', 'VIDEO')),
    content TEXT,
    reply_to_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_message_container CHECK (
        (conversation_id IS NOT NULL AND channel_id IS NULL) OR
        (conversation_id IS NULL AND channel_id IS NOT NULL)
    )
);

-- ----------------------------------------------------------------------------
-- Table: message_attachments
-- Purpose: Object metadata for files/images attached to messages
-- ----------------------------------------------------------------------------
CREATE TABLE message_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    storage_key VARCHAR(500) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 5. COMPANY BROADCAST DOMAIN (Announcements & Acknowledgement Tracking)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: announcements
-- Purpose: Official corporate communications, policy broadcasts, emergency notices
-- ----------------------------------------------------------------------------
CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('NORMAL', 'IMPORTANT', 'URGENT', 'EMERGENCY')),
    audience_type VARCHAR(30) NOT NULL DEFAULT 'ALL_EMPLOYEES' CHECK (audience_type IN ('ALL_EMPLOYEES', 'DEPARTMENT', 'TEAM', 'LOCATION', 'ROLE')),
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    target_location VARCHAR(100),
    target_role_id UUID REFERENCES roles(id) ON DELETE SET NULL,
    published_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    requires_acknowledgement BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Table: announcement_reads
-- Purpose: Auditable tracking of employee reads and explicit acknowledgements
-- ----------------------------------------------------------------------------
CREATE TABLE announcement_reads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMPTZ,
    CONSTRAINT uq_announcement_user UNIQUE (announcement_id, user_id)
);

-- ============================================================================
-- 6. DOCUMENT MANAGEMENT DOMAIN (Documents & Operational Resources)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: documents
-- Purpose: Metadata and object store pointers for logistics policies, SOPs, files
-- ----------------------------------------------------------------------------
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    file_url VARCHAR(1000) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum VARCHAR(64),
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'INTERNAL' CHECK (visibility IN ('PUBLIC', 'INTERNAL', 'DEPARTMENT', 'TEAM', 'CONFIDENTIAL')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 7. CALENDAR & COLLABORATION DOMAIN (Meetings & Participants)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: meetings
-- Purpose: Shift handoffs, team syncs, and operational meetings
-- ----------------------------------------------------------------------------
CREATE TABLE meetings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    organizer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    location VARCHAR(255),
    meeting_link VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'RESCHEDULED')),
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_meeting_duration CHECK (end_time > start_time)
);

-- ----------------------------------------------------------------------------
-- Table: meeting_participants
-- Purpose: Meeting invitations, attendance statuses, and RSVP confirmations
-- ----------------------------------------------------------------------------
CREATE TABLE meeting_participants (
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    response VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (response IN ('PENDING', 'ACCEPTED', 'DECLINED', 'TENTATIVE')),
    joined_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (meeting_id, user_id)
);

-- ============================================================================
-- 8. NOTIFICATIONS DOMAIN
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: notifications
-- Purpose: In-app and real-time event notifications for users
-- ----------------------------------------------------------------------------
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 9. ENTERPRISE AUDIT & COMPLIANCE DOMAIN
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: audit_logs
-- Purpose: Immutable security audit trail with structured JSON context
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 10. INDEXES (High-Frequency Queries Optimization)
-- ============================================================================

-- Organizational Domain Indexes
CREATE INDEX idx_departments_manager_id ON departments(manager_id);
CREATE INDEX idx_teams_department_id ON teams(department_id);
CREATE INDEX idx_teams_team_lead_id ON teams(team_lead_id);
CREATE INDEX idx_employees_dept_id ON employees(department_id);
CREATE INDEX idx_employees_team_id ON employees(team_id);
CREATE INDEX idx_employees_manager_id ON employees(manager_id);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_employees_location ON employees(location);

-- Authentication Domain Indexes
CREATE INDEX idx_users_employee_id ON users(employee_id);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_role_permissions_perm_id ON role_permissions(permission_id);

-- Messaging Domain Indexes
CREATE INDEX idx_conversations_last_msg_at ON conversations(last_message_at DESC NULLS LAST);
CREATE INDEX idx_conversation_members_user_id ON conversation_members(user_id);
CREATE INDEX idx_channels_dept_id ON channels(department_id);
CREATE INDEX idx_channels_team_id ON channels(team_id);
CREATE INDEX idx_channels_type_status ON channels(type, status);
CREATE INDEX idx_channel_members_user_id ON channel_members(user_id);

-- Optimized Paginated Message Queries (Filtered Partial Indexes for Active Messages)
CREATE INDEX idx_messages_conv_created ON messages(conversation_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_messages_chan_created ON messages(channel_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_reply_to ON messages(reply_to_message_id);
CREATE INDEX idx_message_attachments_msg_id ON message_attachments(message_id);

-- Announcements Domain Indexes
CREATE INDEX idx_announcements_pub_status ON announcements(published_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_announcements_dept_id ON announcements(department_id);
CREATE INDEX idx_announcements_team_id ON announcements(team_id);
CREATE INDEX idx_announcements_status ON announcements(status);
CREATE INDEX idx_announcement_reads_user_id ON announcement_reads(user_id);
CREATE INDEX idx_announcement_reads_ack ON announcement_reads(announcement_id, acknowledged_at);

-- Documents Domain Indexes
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_dept_team ON documents(department_id, team_id);
CREATE INDEX idx_documents_visibility_status ON documents(visibility, status) WHERE status = 'ACTIVE';

-- Meetings Domain Indexes
CREATE INDEX idx_meetings_organizer_id ON meetings(organizer_id);
CREATE INDEX idx_meetings_start_time ON meetings(start_time);
CREATE INDEX idx_meetings_status ON meetings(status);
CREATE INDEX idx_meeting_participants_user_resp ON meeting_participants(user_id, response);

-- Notifications Domain Indexes
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, created_at DESC) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_user_all ON notifications(user_id, created_at DESC);

-- Audit Logs Domain Indexes
CREATE INDEX idx_audit_logs_actor_created ON audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_metadata_gin ON audit_logs USING GIN (metadata);

-- ============================================================================
-- 11. AUTOMATIC UPDATED_AT TRIGGERS
-- ============================================================================

CREATE TRIGGER trg_departments_updated_at BEFORE UPDATE ON departments FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_teams_updated_at BEFORE UPDATE ON teams FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_employees_updated_at BEFORE UPDATE ON employees FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_roles_updated_at BEFORE UPDATE ON roles FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_permissions_updated_at BEFORE UPDATE ON permissions FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_conversations_updated_at BEFORE UPDATE ON conversations FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_channels_updated_at BEFORE UPDATE ON channels FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_announcements_updated_at BEFORE UPDATE ON announcements FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_documents_updated_at BEFORE UPDATE ON documents FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_meetings_updated_at BEFORE UPDATE ON meetings FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_meeting_participants_updated_at BEFORE UPDATE ON meeting_participants FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
