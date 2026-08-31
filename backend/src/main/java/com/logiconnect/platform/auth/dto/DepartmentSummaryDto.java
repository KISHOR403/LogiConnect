package com.logiconnect.platform.auth.dto;

import java.util.UUID;

public record DepartmentSummaryDto(
        UUID id,
        String code,
        String name
) {}
