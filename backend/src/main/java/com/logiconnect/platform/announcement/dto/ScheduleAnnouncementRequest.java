package com.logiconnect.platform.announcement.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ScheduleAnnouncementRequest(
        @NotNull(message = "scheduledAt is required")
        Instant scheduledAt
) {
}
