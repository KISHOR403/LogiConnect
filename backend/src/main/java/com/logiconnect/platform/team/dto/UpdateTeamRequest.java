package com.logiconnect.platform.team.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for updating an existing team (PATCH semantics).
 */
public record UpdateTeamRequest(

        @Size(max = 100, message = "Team name must not exceed 100 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        UUID teamLeadId,

        String status
) {}
