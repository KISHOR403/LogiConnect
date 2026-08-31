package com.logiconnect.platform.message.dto;

import java.util.UUID;

/**
 * Public sender profile embedded in message responses.
 */
public record MessageSenderResponse(
        UUID id,
        String employeeCode,
        String name,
        String firstName,
        String lastName,
        String profilePhotoUrl,
        String designation
) {}
