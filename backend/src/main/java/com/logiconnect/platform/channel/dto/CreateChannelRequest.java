package com.logiconnect.platform.channel.dto;

import com.logiconnect.platform.channel.entity.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateChannelRequest(
        @NotBlank(message = "Channel name is required")
        @Size(min = 2, max = 100, message = "Channel name must be between 2 and 100 characters")
        String name,

        @Size(max = 120, message = "Channel slug cannot exceed 120 characters")
        String slug,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @NotNull(message = "Channel type is required")
        ChannelType type,

        UUID departmentId,

        UUID teamId,

        Boolean isReadOnly
) {
}
