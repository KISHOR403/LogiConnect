package com.logiconnect.platform.employee.entity;

/**
 * Corporate employee employment status.
 * Maps to database check constraint: CHECK (status IN ('ACTIVE', 'PROBATION', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED', 'RESIGNED'))
 */
public enum EmployeeStatus {
    ACTIVE,
    PROBATION,
    ON_LEAVE,
    SUSPENDED,
    TERMINATED,
    RESIGNED;

    /**
     * Determines whether an employee with this status is eligible for system access.
     */
    public boolean isEligibleForLogin() {
        return this == ACTIVE || this == PROBATION || this == ON_LEAVE;
    }
}
