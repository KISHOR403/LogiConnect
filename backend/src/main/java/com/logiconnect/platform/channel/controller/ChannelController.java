package com.logiconnect.platform.channel.controller;

import com.logiconnect.platform.channel.dto.*;
import com.logiconnect.platform.channel.entity.ChannelStatus;
import com.logiconnect.platform.channel.entity.ChannelType;
import com.logiconnect.platform.channel.service.ChannelService;
import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/channels")
@Tag(name = "Channels", description = "Company, Department, Team, and Private organizational communication channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping
    @Operation(summary = "Create organizational channel", description = "Creates a new Company, Department, Team, or Private channel (authorized administrators and leaders only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @Valid @RequestBody CreateChannelRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ChannelResponse response = channelService.createChannel(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Channel created successfully"));
    }

    @GetMapping
    @Operation(summary = "List discoverable channels", description = "Returns a paginated list of organizational channels eligible for the authenticated user", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<ChannelResponse>>> listChannels(
            @RequestParam(required = false) ChannelType type,
            @RequestParam(required = false) ChannelStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        PageResponse<ChannelResponse> response = channelService.listChannels(type, status, search, page, size, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Channels retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get channel by ID", description = "Retrieves channel details, status, and members for authorized users", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannelById(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ChannelResponse response = channelService.getChannelById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Channel retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update channel", description = "Updates channel metadata or status (Channel Admin only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannel(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateChannelRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ChannelResponse response = channelService.updateChannel(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Channel updated successfully"));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Join public channel", description = "Allows eligible employees to self-join public organizational channels", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ChannelMemberResponse>> joinChannel(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ChannelMemberResponse response = channelService.joinChannel(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Joined channel successfully"));
    }

    @DeleteMapping("/{id}/members/me")
    @Operation(summary = "Leave channel", description = "Allows member to leave a channel", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> leaveChannel(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        channelService.removeMember(id, currentUserId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "You have left the channel"));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List channel members", description = "Returns active participants belonging to this channel", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<List<ChannelMemberResponse>>> getChannelMembers(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        List<ChannelMemberResponse> response = channelService.getChannelMembers(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Channel members retrieved successfully"));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add member to channel", description = "Adds an active employee to the channel (Channel Admin only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ChannelMemberResponse>> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddChannelMemberRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ChannelMemberResponse response = channelService.addMember(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Member added to channel successfully"));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove member from channel", description = "Removes a member from the channel (Channel Admin only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        channelService.removeMember(id, userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed from channel successfully"));
    }
}
