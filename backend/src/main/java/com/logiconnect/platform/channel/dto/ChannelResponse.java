package com.logiconnect.platform.channel.dto;

import com.logiconnect.platform.channel.entity.ChannelMemberRole;
import com.logiconnect.platform.channel.entity.ChannelStatus;
import com.logiconnect.platform.channel.entity.ChannelType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChannelResponse(
        UUID id,
        String name,
        String slug,
        String description,
        ChannelType type,
        UUID departmentId,
        String departmentName,
        UUID teamId,
        String teamName,
        UUID createdById,
        String createdByName,
        ChannelStatus status,
        boolean isReadOnly,
        long memberCount,
        boolean isMember,
        ChannelMemberRole currentUserRole,
        Instant createdAt,
        Instant updatedAt,
        List<ChannelMemberResponse> members
) {
}
