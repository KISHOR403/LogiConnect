package com.logiconnect.platform.channel.dto;

import com.logiconnect.platform.channel.entity.ChannelMemberRole;

import java.time.Instant;
import java.util.UUID;

public record ChannelMemberResponse(
        UUID channelId,
        UUID userId,
        String employeeCode,
        String fullName,
        String firstName,
        String lastName,
        String profilePhotoUrl,
        String designation,
        String departmentName,
        ChannelMemberRole role,
        Instant joinedAt,
        boolean isMuted
) {
}
