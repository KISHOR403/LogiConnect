package com.logiconnect.platform.announcement.dto;

import com.logiconnect.platform.announcement.entity.AnnouncementPriority;
import com.logiconnect.platform.announcement.entity.AnnouncementTargetType;
import com.logiconnect.platform.announcement.entity.AnnouncementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateAnnouncementRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        AnnouncementType type,

        AnnouncementPriority priority,

        @NotNull(message = "targetType is required")
        AnnouncementTargetType targetType,

        UUID departmentId,

        UUID teamId,

        Boolean requiresAcknowledgement,

        Instant scheduledAt,

        Instant expiresAt
) {
    public AnnouncementType resolvedType() {
        return type != null ? type : AnnouncementType.GENERAL;
    }

    public AnnouncementPriority resolvedPriority() {
        return priority != null ? priority : AnnouncementPriority.NORMAL;
    }

    public boolean resolvedRequiresAcknowledgement() {
        return requiresAcknowledgement != null && requiresAcknowledgement;
    }
}
