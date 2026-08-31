package com.logiconnect.platform.announcement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnnouncementReadResponse(
        UUID announcementId,
        UUID userId,
        boolean read,
        Instant readAt,
        boolean acknowledged,
        Instant acknowledgedAt
) {
}
