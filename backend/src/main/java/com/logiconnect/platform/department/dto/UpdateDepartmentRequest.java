package com.logiconnect.platform.department.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for updating an existing department.
 * All fields are optional — only non-null values are applied (PATCH semantics).
 */
public record UpdateDepartmentRequest(

        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        UUID managerId,

        String status
) {}
