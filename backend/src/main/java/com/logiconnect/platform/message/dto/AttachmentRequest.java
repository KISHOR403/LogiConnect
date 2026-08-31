package com.logiconnect.platform.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload containing verified storage metadata for a file attachment.
 */
public record AttachmentRequest(
        @NotBlank(message = "Storage key is required")
        @Size(max = 500)
        String storageKey,

        @NotBlank(message = "File URL is required")
        @Size(max = 1000)
        String fileUrl,

        @NotBlank(message = "File name is required")
        @Size(max = 255)
        String fileName,

        @NotBlank(message = "File type is required")
        @Size(max = 100)
        String fileType,

        @NotNull(message = "File size is required")
        Long fileSize
) {}
