package com.logiconnect.platform.auth.dto;

import java.util.List;
import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String employeeCode,
        String name,
        String email,
        List<String> roles,
        List<String> permissions
) {}
