-- V8: Seed Initial Users, Roles, and Permissions for Local Development

-- 1. Create Roles if not present
INSERT INTO roles (id, name, display_name, description, is_system) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Super Administrator', 'Super Administrator with full platform control', true),
    ('00000000-0000-0000-0000-000000000002', 'HR_ADMIN', 'HR Administrator', 'HR Administrator for user and employee management', true),
    ('00000000-0000-0000-0000-000000000003', 'MANAGER', 'Department Manager', 'Department Manager', true),
    ('00000000-0000-0000-0000-000000000004', 'TEAM_LEADER', 'Team Leader', 'Operational Team Lead', true),
    ('00000000-0000-0000-0000-000000000005', 'EMPLOYEE', 'Employee', 'Standard Employee', true)
ON CONFLICT (name) DO UPDATE SET display_name = EXCLUDED.display_name;

-- 2. Create Default Department and Team
INSERT INTO departments (id, code, name) VALUES
    ('10000000-0000-0000-0000-000000000001', 'OPS', 'Logistics Operations'),
    ('10000000-0000-0000-0000-000000000002', 'MGMT', 'Executive Management'),
    ('10000000-0000-0000-0000-000000000003', 'HR', 'Human Resources')
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (id, department_id, code, name) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'BLR_HUB', 'Bangalore Central Hub'),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'MUM_DISPATCH', 'Mumbai Dispatch Hub')
ON CONFLICT (code) DO NOTHING;

-- 3. Create Default Employees
INSERT INTO employees (id, employee_code, first_name, last_name, email, designation, location, joining_date, status, department_id, team_id) VALUES
    ('30000000-0000-0000-0000-000000000001', 'ADM1001', 'Admin', 'User', 'admin@logiconnect.internal', 'System Administrator', 'Headquarters', '2023-01-01', 'ACTIVE', '10000000-0000-0000-0000-000000000002', NULL),
    ('30000000-0000-0000-0000-000000000002', 'HR1001', 'Priya', 'Nair', 'hr@logiconnect.internal', 'HR Lead', 'Headquarters', '2023-02-01', 'ACTIVE', '10000000-0000-0000-0000-000000000003', NULL),
    ('30000000-0000-0000-0000-000000000003', 'MGR1001', 'Vikram', 'Malhotra', 'manager@logiconnect.internal', 'Operations Director', 'Bangalore', '2023-01-15', 'ACTIVE', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001'),
    ('30000000-0000-0000-0000-000000000004', 'EMP10001', 'John', 'Doe', 'john.doe@logiconnect.internal', 'Logistics Specialist', 'Bangalore', '2023-03-01', 'ACTIVE', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001')
ON CONFLICT (email) DO NOTHING;

-- 4. Create User Authentication Accounts (Password: Password@1234)
INSERT INTO users (id, employee_id, email, password_hash, status, failed_login_attempts) VALUES
    ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'admin@logiconnect.internal', '$2a$12$UzltzLQQK9UOqMEyh6lj6OZr3hdUq0uZcK.BqXRf1Fuj72g3phNU.', 'ACTIVE', 0),
    ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'hr@logiconnect.internal', '$2a$12$UzltzLQQK9UOqMEyh6lj6OZr3hdUq0uZcK.BqXRf1Fuj72g3phNU.', 'ACTIVE', 0),
    ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', 'manager@logiconnect.internal', '$2a$12$UzltzLQQK9UOqMEyh6lj6OZr3hdUq0uZcK.BqXRf1Fuj72g3phNU.', 'ACTIVE', 0),
    ('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', 'john.doe@logiconnect.internal', '$2a$12$UzltzLQQK9UOqMEyh6lj6OZr3hdUq0uZcK.BqXRf1Fuj72g3phNU.', 'ACTIVE', 0)
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash;

-- 5. Assign User Roles
INSERT INTO user_roles (user_id, role_id)
SELECT '40000000-0000-0000-0000-000000000001', id FROM roles WHERE name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '40000000-0000-0000-0000-000000000002', id FROM roles WHERE name = 'HR_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '40000000-0000-0000-0000-000000000003', id FROM roles WHERE name = 'MANAGER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '40000000-0000-0000-0000-000000000004', id FROM roles WHERE name = 'EMPLOYEE'
ON CONFLICT DO NOTHING;
