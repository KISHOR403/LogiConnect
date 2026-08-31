package com.logiconnect.platform.message.dto;

import com.logiconnect.platform.message.entity.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Message representation returned in chat timelines and single message lookups.
 */
public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID channelId,
        MessageSenderResponse sender,
        MessageType messageType,
        String content,
        UUID replyToMessageId,
        MessageResponse replyToMessage,
        List<AttachmentResponse> attachments,
        boolean isPinned,
        boolean isDeleted,
        Instant createdAt,
        Instant editedAt,
        Instant deletedAt
) {}
