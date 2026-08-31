package com.logiconnect.platform.conversation.dto;

import com.logiconnect.platform.message.entity.MessageType;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary of the latest message in a conversation.
 */
public record LastMessageResponse(
        UUID id,
        UUID senderId,
        String senderName,
        String content,
        MessageType messageType,
        Instant createdAt
) {}
