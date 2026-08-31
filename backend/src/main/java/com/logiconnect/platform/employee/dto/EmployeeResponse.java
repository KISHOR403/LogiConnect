package com.logiconnect.platform.employee.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Safe employee directory response DTO.
 * Strictly zero passwords, hashes, lock timestamps, or security fields.
 */
public record EmployeeResponse(
        UUID id,
        String employeeCode,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        String profilePhotoUrl,
        String designation,
        String departmentId,
        String departmentName,
        String teamId,
        String teamName,
        UUID managerId,
        String managerName,
        String location,
        String status,
        LocalDate joiningDate,
        LocalDate exitDate,
        Instant createdAt,
        Instant updatedAt
) {}
