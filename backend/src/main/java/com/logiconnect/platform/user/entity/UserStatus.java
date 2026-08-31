package com.logiconnect.platform.user.entity;

/**
 * User account lifecycle and security status.
 * Maps to database check constraint: CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_VERIFICATION'))
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    PENDING_VERIFICATION
}
