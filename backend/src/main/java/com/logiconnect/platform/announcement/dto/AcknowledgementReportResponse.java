package com.logiconnect.platform.announcement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.logiconnect.platform.announcement.entity.AnnouncementTargetType;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AcknowledgementReportResponse(
        UUID announcementId,
        String title,
        AnnouncementTargetType targetType,
        boolean requiresAcknowledgement,
        long totalEligible,
        long readCount,
        long acknowledgedCount,
        long pendingCount,
        double acknowledgementRate,
        List<EmployeeAcknowledgementStatusDto> employeeStatuses
) {
}
