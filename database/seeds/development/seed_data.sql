-- ============================================================================
-- LogiConnect Platform - Development Seed Data
-- Location: database/seeds/development/seed_data.sql
-- Description: Minimal realistic development fixtures (Strictly non-production test data)
-- Passwords: All dev accounts use 'Password@123' (BCrypt: $2a$10$eAccYoNOz212j2KwquBoP.Hg5L/1J22k77bK5wLgVpI8YkYg4zFty)
-- ============================================================================

-- 1. SEED ROLES
INSERT INTO roles (id, name, display_name, description, is_system) VALUES
('a0000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Super Administrator', 'Full unrestricted platform and infrastructure administration', TRUE),
('a0000000-0000-0000-0000-000000000002', 'HR_ADMIN', 'HR Administrator', 'Manages employee directory, onboarding, departments, and organization policies', TRUE),
('a0000000-0000-0000-0000-000000000003', 'MANAGER', 'Department Manager', 'Manages department members, teams, broadcasts, and operational resources', TRUE),
('a0000000-0000-0000-0000-000000000004', 'TEAM_LEADER', 'Team Leader', 'Leads operational shifts, logistics hub teams, and shift handoffs', TRUE),
('a0000000-0000-0000-0000-000000000005', 'EMPLOYEE', 'Employee', 'Standard company collaborator with access to assigned teams and channels', TRUE)
ON CONFLICT (name) DO NOTHING;

-- 2. SEED PERMISSIONS
INSERT INTO permissions (id, name, display_name, module, description) VALUES
('b0000000-0000-0000-0000-000000000001', 'MANAGE_EMPLOYEES', 'Manage Employees', 'EMPLOYEE', 'Create, update, and manage employee profiles and offboarding'),
('b0000000-0000-0000-0000-000000000002', 'VIEW_EMPLOYEES', 'View Employee Directory', 'EMPLOYEE', 'Access company organizational directory'),
('b0000000-0000-0000-0000-000000000003', 'CREATE_ANNOUNCEMENT', 'Create Announcement', 'ANNOUNCEMENT', 'Author official corporate announcements'),
('b0000000-0000-0000-0000-000000000004', 'EDIT_ANNOUNCEMENT', 'Edit Announcement', 'ANNOUNCEMENT', 'Modify existing corporate announcements'),
('b0000000-0000-0000-0000-000000000005', 'DELETE_ANNOUNCEMENT', 'Delete Announcement', 'ANNOUNCEMENT', 'Retract and archive corporate announcements'),
('b0000000-0000-0000-0000-000000000006', 'ACKNOWLEDGE_ANNOUNCEMENT', 'Acknowledge Announcement', 'ANNOUNCEMENT', 'Mark mandatory notices as read and acknowledged'),
('b0000000-0000-0000-0000-000000000007', 'CREATE_CHANNEL', 'Create Channels', 'CHANNEL', 'Create public and private organizational channels'),
('b0000000-0000-0000-0000-000000000008', 'MANAGE_CHANNEL', 'Manage Channels', 'CHANNEL', 'Manage channel settings, archives, and moderators'),
('b0000000-0000-0000-0000-000000000009', 'SEND_MESSAGE', 'Send Chat Messages', 'CONVERSATION', 'Participate in direct, group, and channel chats'),
('b0000000-0000-0000-0000-000000000010', 'DELETE_MESSAGE', 'Delete Messages', 'CONVERSATION', 'Delete owned messages or moderate chat messages'),
('b0000000-0000-0000-0000-000000000011', 'CREATE_MEETING', 'Create Meetings', 'MEETING', 'Schedule team syncs, shift handoffs, and video meetings'),
('b0000000-0000-0000-0000-000000000012', 'MANAGE_MEETING', 'Manage Meetings', 'MEETING', 'Reschedule or cancel scheduled meetings'),
('b0000000-0000-0000-0000-000000000013', 'MANAGE_DOCUMENTS', 'Manage Documents', 'DOCUMENT', 'Upload, classify, and archive company documents and SOPs'),
('b0000000-0000-0000-0000-000000000014', 'VIEW_DOCUMENTS', 'View Documents', 'DOCUMENT', 'Access authorized documents and resources'),
('b0000000-0000-0000-0000-000000000015', 'VIEW_AUDIT_LOGS', 'View Audit Logs', 'AUDIT', 'Inspect security and data mutation audit trails'),
('b0000000-0000-0000-0000-000000000016', 'MANAGE_ROLES', 'Manage Roles and Permissions', 'SYSTEM', 'Assign and configure security roles')
ON CONFLICT (name) DO NOTHING;

-- 3. ASSIGN PERMISSIONS TO ROLES
-- Super Admin -> All Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000001', id FROM permissions
ON CONFLICT DO NOTHING;

-- HR Admin -> Employee, Announcement, Document, Meeting
INSERT INTO role_permissions (role_id, permission_id) VALUES
('a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001'),
('a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002'),
('a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000003'),
('a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000004'),
('a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000006'),
('a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000009'),
('a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000011'),
('a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000013'),
('a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000014')
ON CONFLICT DO NOTHING;

-- Manager -> Announcements, Channels, Meetings, Documents
INSERT INTO role_permissions (role_id, permission_id) VALUES
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002'),
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003'),
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000006'),
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000007'),
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000008'),
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000009'),
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000011'),
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000012'),
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000013'),
('a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000014')
ON CONFLICT DO NOTHING;

-- Standard Employee -> Base view and participation permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('a0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000002'),
('a0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000006'),
('a0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000009'),
('a0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000011'),
('a0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000014')
ON CONFLICT DO NOTHING;

-- 4. SEED DEPARTMENTS
INSERT INTO departments (id, code, name, description, status) VALUES
('c0000000-0000-0000-0000-000000000001', 'DEPT-EXEC', 'Executive Leadership', 'Corporate strategy and enterprise administration', 'ACTIVE'),
('c0000000-0000-0000-0000-000000000002', 'DEPT-HR', 'Human Resources', 'Talent acquisition, employee welfare, and HR policies', 'ACTIVE'),
('c0000000-0000-0000-0000-000000000003', 'DEPT-OPS', 'Operations', 'Hub management, fleet operations, line-haul coordination', 'ACTIVE'),
('c0000000-0000-0000-0000-000000000004', 'DEPT-WH', 'Warehouse & Fulfillment', 'Sorting centers, inventory control, and parcel handling', 'ACTIVE'),
('c0000000-0000-0000-0000-000000000005', 'DEPT-CS', 'Customer Support', 'Customer resolution, escalation management, and tracking', 'ACTIVE'),
('c0000000-0000-0000-0000-000000000006', 'DEPT-IT', 'IT & Systems', 'Infrastructure, communication platforms, and technical support', 'ACTIVE')
ON CONFLICT (name) DO NOTHING;

-- 5. SEED TEAMS
INSERT INTO teams (id, department_id, code, name, description, status) VALUES
('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000003', 'TEAM-OPS-BLR', 'Bangalore Hub Operations', 'Bangalore regional dispatch and line-haul operations', 'ACTIVE'),
('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000003', 'TEAM-OPS-MUM', 'Mumbai Hub Operations', 'Mumbai regional dispatch and western corridor logistics', 'ACTIVE'),
('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000004', 'TEAM-WH-DEL', 'Delhi Fulfillment Center', 'North regional distribution and parcel sorting hub', 'ACTIVE'),
('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000005', 'TEAM-CS-TIER1', 'Customer Support Tier 1', 'Direct customer and merchant tracking support', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- 6. SEED EMPLOYEES
INSERT INTO employees (id, employee_code, first_name, last_name, email, phone, designation, department_id, team_id, location, joining_date, status) VALUES
('e0000000-0000-0000-0000-000000000001', 'EMP-1001', 'System', 'Admin', 'admin@logiconnect.internal', '+919876543210', 'Enterprise System Administrator', 'c0000000-0000-0000-0000-000000000006', NULL, 'Bangalore HQ', '2024-01-01', 'ACTIVE'),
('e0000000-0000-0000-0000-000000000002', 'EMP-1002', 'Priya', 'Sharma', 'priya.sharma@logiconnect.internal', '+919876543211', 'HR Director', 'c0000000-0000-0000-0000-000000000002', NULL, 'Bangalore HQ', '2024-01-15', 'ACTIVE'),
('e0000000-0000-0000-0000-000000000003', 'EMP-1003', 'Rajesh', 'Kumar', 'rajesh.kumar@logiconnect.internal', '+919876543212', 'General Manager Operations', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000001', 'Bangalore Hub', '2024-02-01', 'ACTIVE'),
('e0000000-0000-0000-0000-000000000004', 'EMP-1004', 'Amit', 'Verma', 'amit.verma@logiconnect.internal', '+919876543213', 'Warehouse Shift Lead', 'c0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000003', 'Delhi Fulfillment Center', '2024-03-01', 'ACTIVE'),
('e0000000-0000-0000-0000-000000000005', 'EMP-1005', 'Sneha', 'Patil', 'sneha.patil@logiconnect.internal', '+919876543214', 'Customer Support Executive', 'c0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000004', 'Mumbai Office', '2024-04-01', 'ACTIVE')
ON CONFLICT (employee_code) DO NOTHING;

-- Update Department Managers
UPDATE departments SET manager_id = 'e0000000-0000-0000-0000-000000000002' WHERE id = 'c0000000-0000-0000-0000-000000000002';
UPDATE departments SET manager_id = 'e0000000-0000-0000-0000-000000000003' WHERE id = 'c0000000-0000-0000-0000-000000000003';
UPDATE departments SET manager_id = 'e0000000-0000-0000-0000-000000000001' WHERE id = 'c0000000-0000-0000-0000-000000000006';

-- Update Team Leads
UPDATE teams SET team_lead_id = 'e0000000-0000-0000-0000-000000000003' WHERE id = 'd0000000-0000-0000-0000-000000000001';
UPDATE teams SET team_lead_id = 'e0000000-0000-0000-0000-000000000004' WHERE id = 'd0000000-0000-0000-0000-000000000003';

-- 7. SEED USERS (Authentication Accounts)
-- Password for all seed accounts: 'Password@123'
INSERT INTO users (id, employee_id, email, password_hash, status) VALUES
('f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'admin@logiconnect.internal', '$2a$10$eAccYoNOz212j2KwquBoP.Hg5L/1J22k77bK5wLgVpI8YkYg4zFty', 'ACTIVE'),
('f0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000002', 'priya.sharma@logiconnect.internal', '$2a$10$eAccYoNOz212j2KwquBoP.Hg5L/1J22k77bK5wLgVpI8YkYg4zFty', 'ACTIVE'),
('f0000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000003', 'rajesh.kumar@logiconnect.internal', '$2a$10$eAccYoNOz212j2KwquBoP.Hg5L/1J22k77bK5wLgVpI8YkYg4zFty', 'ACTIVE'),
('f0000000-0000-0000-0000-000000000004', 'e0000000-0000-0000-0000-000000000004', 'amit.verma@logiconnect.internal', '$2a$10$eAccYoNOz212j2KwquBoP.Hg5L/1J22k77bK5wLgVpI8YkYg4zFty', 'ACTIVE'),
('f0000000-0000-0000-0000-000000000005', 'e0000000-0000-0000-0000-000000000005', 'sneha.patil@logiconnect.internal', '$2a$10$eAccYoNOz212j2KwquBoP.Hg5L/1J22k77bK5wLgVpI8YkYg4zFty', 'ACTIVE')
ON CONFLICT (email) DO NOTHING;

-- 8. ASSIGN USER ROLES
INSERT INTO user_roles (user_id, role_id) VALUES
('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001'), -- Admin -> SUPER_ADMIN
('f0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002'), -- Priya -> HR_ADMIN
('f0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003'), -- Rajesh -> MANAGER
('f0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004'), -- Amit -> TEAM_LEADER
('f0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005')  -- Sneha -> EMPLOYEE
ON CONFLICT DO NOTHING;

-- 9. SEED DEFAULT CHANNELS
INSERT INTO channels (id, name, slug, description, type, status, is_read_only, created_by) VALUES
('10000000-0000-0000-0000-000000000001', 'Company Announcements', 'company-announcements', 'Official corporate updates and management broadcasts', 'COMPANY', 'ACTIVE', TRUE, 'f0000000-0000-0000-0000-000000000001'),
('10000000-0000-0000-0000-000000000002', 'General Discussion', 'general-discussion', 'All-hands watercooler and general collaboration space', 'COMPANY', 'ACTIVE', FALSE, 'f0000000-0000-0000-0000-000000000001'),
('10000000-0000-0000-0000-000000000003', 'Operations Dispatch', 'operations-dispatch', 'Real-time operational alerts, hub status, and route updates', 'DEPARTMENT', 'ACTIVE', FALSE, 'f0000000-0000-0000-0000-000000000003'),
('10000000-0000-0000-0000-000000000004', 'IT Helpdesk & Incident', 'it-helpdesk', 'Technical support, system outages, and infrastructure alerts', 'COMPANY', 'ACTIVE', FALSE, 'f0000000-0000-0000-0000-000000000001')
ON CONFLICT (slug) DO NOTHING;

-- Associate channel department
UPDATE channels SET department_id = 'c0000000-0000-0000-0000-000000000003' WHERE id = '10000000-0000-0000-0000-000000000003';

-- 10. SEED SAMPLE ANNOUNCEMENT (With Acknowledgement Requirement)
INSERT INTO announcements (id, title, content, created_by, priority, audience_type, published_at, requires_acknowledgement, status) VALUES
('20000000-0000-0000-0000-000000000001', 'Welcome to LogiConnect Enterprise Platform', 'LogiConnect is now the official and secure communication standard for all corporate logistics operations, replacing external messaging tools. Please review the security policy and acknowledge receipt.', 'f0000000-0000-0000-0000-000000000002', 'IMPORTANT', 'ALL_EMPLOYEES', CURRENT_TIMESTAMP, TRUE, 'PUBLISHED')
ON CONFLICT DO NOTHING;

-- 11. SEED SAMPLE ANNOUNCEMENT READ & ACKNOWLEDGEMENT
INSERT INTO announcement_reads (id, announcement_id, user_id, read_at, acknowledged_at) VALUES
('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000004', CURRENT_TIMESTAMP, NULL)
ON CONFLICT DO NOTHING;
