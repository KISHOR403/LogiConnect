package com.logiconnect.platform.conversation.dto;

import com.logiconnect.platform.conversation.entity.ConversationMemberRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe member profile returned in conversation details and member lists.
 */
public record ConversationMemberResponse(
        UUID userId,
        String employeeCode,
        String name,
        String firstName,
        String lastName,
        String email,
        String profilePhotoUrl,
        String designation,
        ConversationMemberRole role,
        Instant joinedAt,
        boolean isMuted,
        boolean isPinned
) {}
