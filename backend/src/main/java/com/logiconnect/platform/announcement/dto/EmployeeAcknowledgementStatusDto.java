package com.logiconnect.platform.announcement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmployeeAcknowledgementStatusDto(
        UUID userId,
        UUID employeeId,
        String employeeCode,
        String fullName,
        String designation,
        String departmentName,
        String teamName,
        boolean read,
        Instant readAt,
        boolean acknowledged,
        Instant acknowledgedAt,
        String status
) {
}
