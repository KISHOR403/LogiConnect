package com.logiconnect.platform.message.dto;

import com.logiconnect.platform.message.entity.MessageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for posting a new message to a conversation.
 */
public record SendMessageRequest(
        @Size(max = 10000, message = "Message content must not exceed 10000 characters")
        String content,

        MessageType messageType,

        UUID replyToMessageId,

        @Valid
        List<AttachmentRequest> attachments
) {}
