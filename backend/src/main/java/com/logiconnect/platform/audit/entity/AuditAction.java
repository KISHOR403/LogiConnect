package com.logiconnect.platform.audit.entity;

/**
 * Standard enterprise audit action types.
 */
public enum AuditAction {
    // Authentication lifecycle
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    PASSWORD_CHANGED,
    ACCOUNT_LOCKED,
    TOKEN_REFRESHED,

    // Department lifecycle
    DEPARTMENT_CREATED,
    DEPARTMENT_UPDATED,
    DEPARTMENT_DEACTIVATED,

    // Team lifecycle
    TEAM_CREATED,
    TEAM_UPDATED,
    TEAM_DEACTIVATED,

    // Employee lifecycle
    EMPLOYEE_CREATED,
    EMPLOYEE_UPDATED,
    EMPLOYEE_STATUS_CHANGED,
    EMPLOYEE_DEACTIVATED,

    // Messaging lifecycle
    CONVERSATION_CREATED,
    GROUP_MEMBER_ADDED,
    GROUP_MEMBER_REMOVED,
    MESSAGE_EDITED,
    MESSAGE_DELETED
}
