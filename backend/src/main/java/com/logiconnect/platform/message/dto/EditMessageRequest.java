package com.logiconnect.platform.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for editing message content.
 */
public record EditMessageRequest(
        @NotBlank(message = "Message content cannot be blank")
        @Size(max = 10000, message = "Message content must not exceed 10000 characters")
        String content
) {}
