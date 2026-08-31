package com.logiconnect.platform.auth.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String employeeCode,
        String name,
        String firstName,
        String lastName,
        String email,
        String designation,
        String location,
        String status,
        DepartmentSummaryDto department,
        TeamSummaryDto team,
        List<String> roles,
        List<String> permissions
) {}
