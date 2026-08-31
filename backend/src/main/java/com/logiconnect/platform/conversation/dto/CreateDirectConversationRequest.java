package com.logiconnect.platform.conversation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for initiating or retrieving a 1-to-1 direct conversation.
 */
public record CreateDirectConversationRequest(
        @NotNull(message = "Target user ID is required")
        UUID targetUserId
) {}
