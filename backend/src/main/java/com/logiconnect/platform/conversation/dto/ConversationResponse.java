package com.logiconnect.platform.conversation.dto;

import com.logiconnect.platform.conversation.entity.ConversationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed conversation response DTO.
 */
public record ConversationResponse(
        UUID id,
        ConversationType type,
        String name,
        String description,
        String avatarUrl,
        UUID createdById,
        String createdByName,
        boolean isArchived,
        long memberCount,
        List<ConversationMemberResponse> members,
        LastMessageResponse lastMessage,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt
) {}
