package com.logiconnect.platform.announcement.controller;

import com.logiconnect.platform.announcement.dto.*;
import com.logiconnect.platform.announcement.entity.AnnouncementStatus;
import com.logiconnect.platform.announcement.entity.AnnouncementType;
import com.logiconnect.platform.announcement.service.AnnouncementService;
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

import java.util.UUID;

@RestController
@RequestMapping("/announcements")
@Tag(name = "Announcements", description = "Official corporate broadcasts, emergency notices, read tracking, and mandatory acknowledgements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    @Operation(summary = "Create official announcement", description = "Drafts a new company, department, or team announcement with targeting authorization checks", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AnnouncementResponse response = announcementService.createAnnouncement(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Announcement created successfully"));
    }

    @GetMapping
    @Operation(summary = "List announcement feed", description = "Returns paginated announcements visible to the authenticated employee based on targeting eligibility", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<AnnouncementResponse>>> listAnnouncements(
            @RequestParam(required = false) AnnouncementType type,
            @RequestParam(required = false) AnnouncementStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        PageResponse<AnnouncementResponse> response = announcementService.listAnnouncements(type, status, search, page, size, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcements retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get announcement details", description = "Retrieves announcement content and implicitly records read timestamp if published", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncementById(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AnnouncementResponse response = announcementService.getAnnouncementById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update announcement", description = "Modifies draft or scheduled announcement metadata before publication", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAnnouncementRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AnnouncementResponse response = announcementService.updateAnnouncement(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement updated successfully"));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish announcement", description = "Transitions draft or scheduled announcement to PUBLISHED state with audit logging", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AnnouncementResponse>> publishAnnouncement(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AnnouncementResponse response = announcementService.publishAnnouncement(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement published successfully"));
    }

    @PostMapping("/{id}/schedule")
    @Operation(summary = "Schedule announcement", description = "Sets future publication time for an announcement", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AnnouncementResponse>> scheduleAnnouncement(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleAnnouncementRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AnnouncementResponse response = announcementService.scheduleAnnouncement(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement scheduled successfully"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel announcement", description = "Cancels an active or draft announcement without physical deletion", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AnnouncementResponse>> cancelAnnouncement(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AnnouncementResponse response = announcementService.cancelAnnouncement(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement cancelled successfully"));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive announcement", description = "Moves published announcement to archived state", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AnnouncementResponse>> archiveAnnouncement(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AnnouncementResponse response = announcementService.archiveAnnouncement(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement archived successfully"));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark announcement as read", description = "Explicitly records read timestamp for the authenticated employee", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AnnouncementReadResponse>> markAsRead(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AnnouncementReadResponse response = announcementService.markAsRead(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement marked as read"));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge mandatory announcement", description = "Records formal non-repudiation employee sign-off", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AnnouncementReadResponse>> acknowledgeAnnouncement(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AnnouncementReadResponse response = announcementService.acknowledgeAnnouncement(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement acknowledged successfully"));
    }

    @GetMapping(value = {"/{id}/acknowledgements", "/{id}/statistics"})
    @Operation(summary = "Get acknowledgement report", description = "Aggregates compliance metrics and employee statuses for authorized leaders/admins", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AcknowledgementReportResponse>> getAcknowledgementReport(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        AcknowledgementReportResponse response = announcementService.getAcknowledgementReport(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Acknowledgement report retrieved successfully"));
    }
}
