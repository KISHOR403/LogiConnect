package com.logiconnect.platform.message.controller;

import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.common.util.SecurityUtils;
import com.logiconnect.platform.message.dto.EditMessageRequest;
import com.logiconnect.platform.message.dto.MessageResponse;
import com.logiconnect.platform.message.dto.SendMessageRequest;
import com.logiconnect.platform.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Messages", description = "Message creation, history retrieval, message editing, soft deletion, thread replies, and pinning")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // ==========================================
    // 1. CONVERSATION MESSAGES
    // ==========================================

    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Send message to conversation", description = "Posts a new message or attachment to a conversation (sender derived strictly from authenticated user)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        MessageResponse response = messageService.sendMessage(conversationId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Message sent successfully"));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "List conversation messages", description = "Returns a paginated list of messages for a conversation ordered chronologically", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> listMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        PageResponse<MessageResponse> response = messageService.listMessages(conversationId, page, size, direction, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Messages retrieved successfully"));
    }

    // ==========================================
    // 2. CHANNEL MESSAGES
    // ==========================================

    @PostMapping("/channels/{channelId}/messages")
    @Operation(summary = "Send message to channel", description = "Posts a new message or attachment to an organizational channel (sender derived strictly from authenticated user)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<MessageResponse>> sendChannelMessage(
            @PathVariable UUID channelId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        MessageResponse response = messageService.sendChannelMessage(channelId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Channel message sent successfully"));
    }

    @GetMapping("/channels/{channelId}/messages")
    @Operation(summary = "List channel messages", description = "Returns a paginated list of messages for an organizational channel ordered chronologically", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> listChannelMessages(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        PageResponse<MessageResponse> response = messageService.listChannelMessages(channelId, page, size, direction, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Channel messages retrieved successfully"));
    }

    @PostMapping("/channels/{channelId}/messages/{messageId}/pin")
    @Operation(summary = "Pin message in channel", description = "Pins a message to the channel header (Channel Admin or Moderator only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<MessageResponse>> pinChannelMessage(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        MessageResponse response = messageService.pinChannelMessage(channelId, messageId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Message pinned successfully"));
    }

    @DeleteMapping("/channels/{channelId}/messages/{messageId}/pin")
    @Operation(summary = "Unpin message in channel", description = "Unpins a message from the channel header (Channel Admin or Moderator only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<MessageResponse>> unpinChannelMessage(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        MessageResponse response = messageService.unpinChannelMessage(channelId, messageId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Message unpinned successfully"));
    }

    // ==========================================
    // 3. COMMON MESSAGE OPERATIONS
    // ==========================================

    @GetMapping("/messages/{id}")
    @Operation(summary = "Get message by ID", description = "Retrieves a single message by ID if authorized", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<MessageResponse>> getMessageById(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        MessageResponse response = messageService.getMessageById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Message retrieved successfully"));
    }

    @PutMapping("/messages/{id}")
    @Operation(summary = "Edit message", description = "Edits message text content (sender only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @PathVariable UUID id,
            @Valid @RequestBody EditMessageRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        MessageResponse response = messageService.editMessage(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Message updated successfully"));
    }

    @DeleteMapping("/messages/{id}")
    @Operation(summary = "Delete message", description = "Soft-deletes a message (sender or channel moderator)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<MessageResponse>> deleteMessage(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        MessageResponse response = messageService.softDeleteMessage(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Message deleted successfully"));
    }
}
