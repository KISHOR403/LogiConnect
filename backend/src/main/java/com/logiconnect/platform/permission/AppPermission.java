package com.logiconnect.platform.permission;

/**
 * Fine-grained platform permission constants aligned with the authorization matrix.
 */
public final class AppPermission {

    private AppPermission() {
    }

    // Employee & Org Permissions
    public static final String VIEW_EMPLOYEES = "VIEW_EMPLOYEES";
    public static final String MANAGE_EMPLOYEES = "MANAGE_EMPLOYEES";
    public static final String VIEW_DEPARTMENTS = "VIEW_DEPARTMENTS";
    public static final String MANAGE_DEPARTMENTS = "MANAGE_DEPARTMENTS";
    public static final String VIEW_TEAMS = "VIEW_TEAMS";
    public static final String MANAGE_TEAMS = "MANAGE_TEAMS";

    // Messaging & Collaboration Permissions
    public static final String SEND_MESSAGES = "SEND_MESSAGES";
    public static final String MANAGE_CHANNELS = "MANAGE_CHANNELS";
    public static final String PUBLISH_ANNOUNCEMENTS = "PUBLISH_ANNOUNCEMENTS";
    public static final String MANAGE_DOCUMENTS = "MANAGE_DOCUMENTS";
    public static final String SCHEDULE_MEETINGS = "SCHEDULE_MEETINGS";

    // Administrative Permissions
    public static final String VIEW_AUDIT_LOGS = "VIEW_AUDIT_LOGS";
    public static final String MANAGE_ROLES = "MANAGE_ROLES";
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
}
