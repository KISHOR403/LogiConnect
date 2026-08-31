package com.logiconnect.platform.conversation.controller;

import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.common.util.SecurityUtils;
import com.logiconnect.platform.conversation.dto.*;
import com.logiconnect.platform.conversation.entity.ConversationType;
import com.logiconnect.platform.conversation.service.ConversationService;
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
@RequestMapping("/conversations")
@Tag(name = "Conversations", description = "Direct 1-to-1 and group conversation rooms, participation, and membership lifecycle")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/direct")
    @Operation(summary = "Initiate or retrieve direct conversation", description = "Creates or retrieves an existing 1-to-1 direct conversation with another employee", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ConversationResponse>> createDirectConversation(
            @Valid @RequestBody CreateDirectConversationRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ConversationResponse response = conversationService.createOrGetDirectConversation(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Direct conversation ready"));
    }

    @PostMapping("/group")
    @Operation(summary = "Create group conversation", description = "Creates an ad-hoc group conversation with multiple team members", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ConversationResponse>> createGroupConversation(
            @Valid @RequestBody CreateGroupConversationRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ConversationResponse response = conversationService.createGroupConversation(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Group conversation created successfully"));
    }

    @GetMapping
    @Operation(summary = "List user conversations", description = "Returns a paginated list of conversations in which the authenticated user is an active participant", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> listMyConversations(
            @RequestParam(required = false) ConversationType type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        PageResponse<ConversationResponse> response = conversationService.listMyConversations(type, search, page, size, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Conversations retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation by ID", description = "Retrieves conversation details and active member profiles for authorized participants", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationById(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ConversationResponse response = conversationService.getConversationById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Conversation retrieved successfully"));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List conversation members", description = "Returns active participants belonging to this conversation", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<List<ConversationMemberResponse>>> getConversationMembers(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        List<ConversationMemberResponse> response = conversationService.getConversationMembers(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Conversation members retrieved successfully"));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add member to group", description = "Adds an active user to the group conversation (Group Admin only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ConversationMemberResponse>> addMemberToGroup(
            @PathVariable UUID id,
            @Valid @RequestBody AddConversationMemberRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ConversationMemberResponse response = conversationService.addMemberToGroup(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Member added successfully"));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove member from group", description = "Removes a participant from the group conversation (Group Admin only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> removeMemberFromGroup(
            @PathVariable UUID id,
            @PathVariable UUID userId
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        conversationService.removeMemberFromGroup(id, userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed successfully"));
    }

    @DeleteMapping("/{id}/members/me")
    @Operation(summary = "Leave group conversation", description = "Removes authenticated user from the group conversation", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> leaveGroup(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        conversationService.removeMemberFromGroup(id, currentUserId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "You have left the conversation"));
    }
}
