package com.logiconnect.platform.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a new organizational department.
 */
public record CreateDepartmentRequest(

        @NotBlank(message = "Department code is required")
        @Size(max = 50, message = "Department code must not exceed 50 characters")
        @Pattern(regexp = "^[A-Z][A-Z0-9_-]*$", message = "Department code must start with a letter and contain only uppercase letters, numbers, hyphens, and underscores")
        String code,

        @NotBlank(message = "Department name is required")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        UUID managerId
) {}
