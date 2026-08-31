package com.logiconnect.platform.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new group conversation room.
 */
public record CreateGroupConversationRequest(
        @NotBlank(message = "Group conversation name is required")
        @Size(max = 150, message = "Group conversation name must not exceed 150 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Size(max = 1000, message = "Avatar URL must not exceed 1000 characters")
        String avatarUrl,

        @NotEmpty(message = "At least one member must be specified")
        List<UUID> memberIds
) {}
