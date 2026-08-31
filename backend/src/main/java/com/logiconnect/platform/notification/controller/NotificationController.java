package com.logiconnect.platform.notification.controller;

import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.common.util.SecurityUtils;
import com.logiconnect.platform.notification.dto.NotificationResponse;
import com.logiconnect.platform.notification.dto.UnreadCountResponse;
import com.logiconnect.platform.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "In-app notifications, event alerts, read tracking, and unread counts")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get employee notifications", description = "Returns paginated notification feed belonging strictly to the authenticated employee", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        PageResponse<NotificationResponse> response = notificationService.getNotifications(
                currentUserId, unreadOnly, PageRequest.of(page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Notifications retrieved successfully"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count", description = "Returns the total number of unread notifications for the authenticated employee", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        UnreadCountResponse response = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Unread count retrieved successfully"));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read for the authenticated employee", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        NotificationResponse response = notificationService.markAsRead(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification marked as read successfully"));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read for the authenticated employee", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead() {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        int count = notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("markedCount", count), "All notifications marked as read successfully"));
    }
}
