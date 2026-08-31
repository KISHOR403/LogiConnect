package com.logiconnect.platform.channel.dto;

import com.logiconnect.platform.channel.entity.ChannelStatus;
import jakarta.validation.constraints.Size;

public record UpdateChannelRequest(
        @Size(min = 2, max = 100, message = "Channel name must be between 2 and 100 characters")
        String name,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        Boolean isReadOnly,

        ChannelStatus status
) {
}
