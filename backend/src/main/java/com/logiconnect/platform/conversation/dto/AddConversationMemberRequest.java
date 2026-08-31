package com.logiconnect.platform.conversation.dto;

import com.logiconnect.platform.conversation.entity.ConversationMemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for adding a new member to a group conversation.
 */
public record AddConversationMemberRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        ConversationMemberRole role
) {}
