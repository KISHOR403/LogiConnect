package com.logiconnect.platform.message.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Attachment response metadata DTO.
 */
public record AttachmentResponse(
        UUID id,
        String storageKey,
        String fileUrl,
        String fileName,
        String fileType,
        long fileSize,
        Instant createdAt
) {}
