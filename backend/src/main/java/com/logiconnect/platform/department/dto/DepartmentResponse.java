package com.logiconnect.platform.department.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary DTO for department list views and dropdowns.
 */
public record DepartmentResponse(
        UUID id,
        String code,
        String name,
        String description,
        String status,
        UUID managerId,
        String managerName,
        long teamCount,
        long employeeCount,
        Instant createdAt,
        Instant updatedAt
) {}
