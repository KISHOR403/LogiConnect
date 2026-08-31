package com.logiconnect.platform.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a new team within a department.
 */
public record CreateTeamRequest(

        @NotNull(message = "Department ID is required")
        UUID departmentId,

        @NotBlank(message = "Team code is required")
        @Size(max = 50, message = "Team code must not exceed 50 characters")
        @Pattern(regexp = "^[A-Z][A-Z0-9_-]*$", message = "Team code must start with a letter and contain only uppercase letters, numbers, hyphens, and underscores")
        String code,

        @NotBlank(message = "Team name is required")
        @Size(max = 100, message = "Team name must not exceed 100 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        UUID teamLeadId
) {}
