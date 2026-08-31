package com.logiconnect.platform.announcement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.logiconnect.platform.announcement.entity.AnnouncementPriority;
import com.logiconnect.platform.announcement.entity.AnnouncementStatus;
import com.logiconnect.platform.announcement.entity.AnnouncementTargetType;
import com.logiconnect.platform.announcement.entity.AnnouncementType;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnnouncementResponse(
        UUID id,
        String title,
        String content,
        AnnouncementType type,
        AnnouncementPriority priority,
        AnnouncementTargetType targetType,
        UUID departmentId,
        String departmentName,
        UUID teamId,
        String teamName,
        AnnouncementStatus status,
        boolean requiresAcknowledgement,
        Instant scheduledAt,
        Instant publishedAt,
        Instant expiresAt,
        UUID createdById,
        String createdByName,
        UUID publishedById,
        String publishedByName,
        Instant createdAt,
        Instant updatedAt,
        boolean read,
        boolean acknowledged,
        Instant readAt,
        Instant acknowledgedAt
) {
}
