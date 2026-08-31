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
@Tag(name = "Messages", description = "Message creation, history retrieval, message editing, soft deletion, and thread replies")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

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
    @Operation(summary = "Delete message", description = "Soft-deletes a message (sender only)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<MessageResponse>> deleteMessage(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        MessageResponse response = messageService.softDeleteMessage(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Message deleted successfully"));
    }
}
