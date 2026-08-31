package com.logiconnect.platform.audit.entity;

/**
 * Standard enterprise audit action types.
 */
public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    PASSWORD_CHANGED,
    ACCOUNT_LOCKED,
    TOKEN_REFRESHED
}
