-- ============================================================================
-- LogiConnect Platform - Database Migration V2
-- Migration: V2__authentication_sessions.sql
-- Description: User session management for JWT refresh token rotation and revocation
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: user_sessions
-- Purpose: Secure server-side tracking of active refresh tokens & device sessions
-- Security: Raw refresh tokens are never stored; only SHA-256 hashes are persisted.
-- ----------------------------------------------------------------------------
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for querying active sessions by user
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);

-- Index for refresh token lookup during token rotation
CREATE INDEX idx_user_sessions_token_hash ON user_sessions(refresh_token_hash);

-- Composite index for fast expiration and revocation checks
CREATE INDEX idx_user_sessions_expires_revoked ON user_sessions(expires_at, revoked_at);

-- Trigger for automatic updated_at timestamp management
CREATE TRIGGER trg_user_sessions_updated_at
    BEFORE UPDATE ON user_sessions
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();
