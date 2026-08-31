package com.logiconnect.platform.channel.dto;

import com.logiconnect.platform.channel.entity.ChannelMemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddChannelMemberRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        ChannelMemberRole role
) {
}
