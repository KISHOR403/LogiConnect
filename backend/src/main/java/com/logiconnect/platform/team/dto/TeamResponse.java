package com.logiconnect.platform.team.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for team list and detail views.
 */
public record TeamResponse(
        UUID id,
        UUID departmentId,
        String departmentName,
        String code,
        String name,
        String description,
        String status,
        UUID teamLeadId,
        String teamLeadName,
        long memberCount,
        Instant createdAt,
        Instant updatedAt
) {}
