-- ============================================================================
-- LogiConnect Platform - Database Migration V6
-- Migration: V6__announcements.sql
-- Description: Official Announcements & Company Broadcast System enhancements
-- ============================================================================

-- 1. ENHANCE ANNOUNCEMENTS TABLE
-- Add announcement type column
ALTER TABLE announcements
    ADD COLUMN IF NOT EXISTS type VARCHAR(30) NOT NULL DEFAULT 'GENERAL';

-- Add scheduled_at column for scheduled publication
ALTER TABLE announcements
    ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMPTZ;

-- Add published_by column to track the user who published/authorized broadcast
ALTER TABLE announcements
    ADD COLUMN IF NOT EXISTS published_by UUID REFERENCES users(id) ON DELETE SET NULL;

-- Drop and update check constraints for status to include 'SCHEDULED'
ALTER TABLE announcements
    DROP CONSTRAINT IF EXISTS announcements_status_check;

ALTER TABLE announcements
    ADD CONSTRAINT chk_announcements_status
    CHECK (status IN ('DRAFT', 'SCHEDULED', 'PUBLISHED', 'CANCELLED', 'ARCHIVED'));

-- Drop and update check constraints for priority to include 'HIGH'
ALTER TABLE announcements
    DROP CONSTRAINT IF EXISTS announcements_priority_check;

ALTER TABLE announcements
    ADD CONSTRAINT chk_announcements_priority
    CHECK (priority IN ('NORMAL', 'HIGH', 'IMPORTANT', 'URGENT', 'EMERGENCY'));

-- Drop and update check constraints for audience_type to include 'COMPANY'
ALTER TABLE announcements
    DROP CONSTRAINT IF EXISTS announcements_audience_type_check;

ALTER TABLE announcements
    ADD CONSTRAINT chk_announcements_audience_type
    CHECK (audience_type IN ('ALL', 'COMPANY', 'DEPARTMENT', 'TEAM', 'ROLE', 'INDIVIDUAL', 'ALL_EMPLOYEES'));

-- Add check constraint for announcement type
ALTER TABLE announcements
    ADD CONSTRAINT chk_announcements_type
    CHECK (type IN ('GENERAL', 'HR', 'OPERATIONS', 'SAFETY', 'POLICY', 'EMERGENCY', 'MAINTENANCE'));

-- 2. ENHANCE ANNOUNCEMENT_READS TABLE
-- Add created_at and updated_at timestamps for tracking read record lifecycle
ALTER TABLE announcement_reads
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE announcement_reads
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 3. INDEXES FOR HIGH-FREQUENCY ACCESS PATTERNS
-- Filtered index for pending scheduled announcements
CREATE INDEX IF NOT EXISTS idx_announcements_scheduled_at
    ON announcements(scheduled_at)
    WHERE status = 'SCHEDULED';

-- Index for filtering announcements by classification type
CREATE INDEX IF NOT EXISTS idx_announcements_type
    ON announcements(type);

-- Composite index for rapid employee read timeline queries
CREATE INDEX IF NOT EXISTS idx_announcement_reads_user_read
    ON announcement_reads(user_id, read_at);

-- Trigger for auto-updating updated_at on announcement_reads
CREATE TRIGGER trg_announcement_reads_updated_at
    BEFORE UPDATE ON announcement_reads
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();
