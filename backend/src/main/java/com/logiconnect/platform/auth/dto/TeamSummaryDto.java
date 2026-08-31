package com.logiconnect.platform.auth.dto;

import java.util.UUID;

public record TeamSummaryDto(
        UUID id,
        String code,
        String name
) {}
