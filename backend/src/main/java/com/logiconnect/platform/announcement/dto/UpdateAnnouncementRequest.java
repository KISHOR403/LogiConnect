package com.logiconnect.platform.announcement.dto;

import com.logiconnect.platform.announcement.entity.AnnouncementPriority;
import com.logiconnect.platform.announcement.entity.AnnouncementTargetType;
import com.logiconnect.platform.announcement.entity.AnnouncementType;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateAnnouncementRequest(
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        String content,

        AnnouncementType type,

        AnnouncementPriority priority,

        AnnouncementTargetType targetType,

        UUID departmentId,

        UUID teamId,

        Boolean requiresAcknowledgement,

        Instant scheduledAt,

        Instant expiresAt
) {
}
