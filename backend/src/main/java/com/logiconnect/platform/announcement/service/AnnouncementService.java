package com.logiconnect.platform.announcement.service;

import com.logiconnect.platform.announcement.dto.*;
import com.logiconnect.platform.announcement.entity.*;
import com.logiconnect.platform.announcement.repository.AnnouncementReadRepository;
import com.logiconnect.platform.announcement.repository.AnnouncementRepository;
import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.service.AuditService;
import com.logiconnect.platform.common.exception.BadRequestException;
import com.logiconnect.platform.common.exception.ConflictException;
import com.logiconnect.platform.common.exception.ForbiddenException;
import com.logiconnect.platform.common.exception.ResourceNotFoundException;
import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.employee.entity.EmployeeStatus;
import com.logiconnect.platform.permission.AppPermission;
import com.logiconnect.platform.team.entity.Team;
import com.logiconnect.platform.team.repository.TeamRepository;
import com.logiconnect.platform.user.entity.User;
import com.logiconnect.platform.user.entity.UserStatus;
import com.logiconnect.platform.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final com.logiconnect.platform.notification.service.NotificationService notificationService;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            AnnouncementReadRepository announcementReadRepository,
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            UserRepository userRepository,
            AuditService auditService,
            com.logiconnect.platform.notification.service.NotificationService notificationService
    ) {
        this.announcementRepository = announcementRepository;
        this.announcementReadRepository = announcementReadRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request, UUID currentUserId) {
        User creator = getActiveUser(currentUserId);

        Department department = null;
        Team team = null;

        // 1. Validate target parameters consistency
        if (request.targetType() == AnnouncementTargetType.COMPANY) {
            if (request.departmentId() != null || request.teamId() != null) {
                throw new BadRequestException("COMPANY announcements must not specify departmentId or teamId");
            }
        } else if (request.targetType() == AnnouncementTargetType.DEPARTMENT) {
            if (request.departmentId() == null) {
                throw new BadRequestException("departmentId is required for DEPARTMENT announcements");
            }
            if (request.teamId() != null) {
                throw new BadRequestException("teamId must not be specified for DEPARTMENT announcements");
            }
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
        } else if (request.targetType() == AnnouncementTargetType.TEAM) {
            if (request.teamId() == null) {
                throw new BadRequestException("teamId is required for TEAM announcements");
            }
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.teamId()));
            department = team.getDepartment();
            if (request.departmentId() != null && !request.departmentId().equals(department.getId())) {
                throw new BadRequestException("Specified departmentId does not match the team's parent department");
            }
        }

        // 2. Server-side targeting authorization
        verifyTargetingAuthorization(creator, request.targetType(), department, team);

        // 3. Validate scheduling / expiration timestamps
        Instant now = Instant.now();
        if (request.scheduledAt() != null) {
            if (!request.scheduledAt().isAfter(now)) {
                throw new BadRequestException("scheduledAt must be in the future");
            }
        }
        if (request.expiresAt() != null) {
            if (!request.expiresAt().isAfter(now)) {
                throw new BadRequestException("expiresAt must be in the future");
            }
            if (request.scheduledAt() != null && !request.expiresAt().isAfter(request.scheduledAt())) {
                throw new BadRequestException("expiresAt must be after scheduledAt");
            }
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());
        announcement.setType(request.resolvedType());
        announcement.setPriority(request.resolvedPriority());
        announcement.setTargetType(request.targetType());
        announcement.setDepartment(department);
        announcement.setTeam(team);
        announcement.setRequiresAcknowledgement(request.resolvedRequiresAcknowledgement());
        announcement.setScheduledAt(request.scheduledAt());
        announcement.setExpiresAt(request.expiresAt());
        announcement.setCreatedBy(creator);

        if (request.scheduledAt() != null) {
            announcement.setStatus(AnnouncementStatus.SCHEDULED);
        } else {
            announcement.setStatus(AnnouncementStatus.DRAFT);
        }

        announcement = announcementRepository.save(announcement);

        log.info("Created announcement: id={}, title='{}', targetType={}, status={}, createdBy={}",
                announcement.getId(), announcement.getTitle(), announcement.getTargetType(), announcement.getStatus(), currentUserId);

        auditService.recordAuthEvent(creator, AuditAction.ANNOUNCEMENT_CREATED, "ANNOUNCEMENT", announcement.getId(), null, null,
                buildSanitizedAuditMetadata(announcement));

        return toAnnouncementResponse(announcement, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> listAnnouncements(
            AnnouncementType type,
            AnnouncementStatus status,
            String search,
            int page,
            int size,
            UUID currentUserId
    ) {
        User user = getActiveUser(currentUserId);
        Employee employee = user.getEmployee();

        int pageSize = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        int pageNumber = Math.max(page, 0);
        Pageable pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        Instant now = Instant.now();

        Page<Announcement> announcementPage;

        if (isSuperAdmin(user) || isHrAdmin(user)) {
            // Admins can filter by any status, or default to all
            announcementPage = announcementRepository.findAllAdmin(status, type, searchTerm, pageable);
        } else if (status != null && (status == AnnouncementStatus.DRAFT || status == AnnouncementStatus.SCHEDULED)) {
            // Non-admin creators can list their own drafts/scheduled notices
            announcementPage = announcementRepository.findByCreator(currentUserId, status, type, searchTerm, pageable);
        } else {
            // Ordinary employees feed: strictly active published announcements targeted to employee
            UUID deptId = (employee != null && employee.getDepartment() != null) ? employee.getDepartment().getId() : null;
            UUID teamId = (employee != null && employee.getTeam() != null) ? employee.getTeam().getId() : null;

            announcementPage = announcementRepository.findFeedForEmployee(deptId, teamId, now, type, searchTerm, pageable);
        }

        // Batch fetch read records for returned page to prevent N+1 queries
        List<UUID> announcementIds = announcementPage.getContent().stream().map(Announcement::getId).toList();
        Map<UUID, AnnouncementRead> readMap = new HashMap<>();
        if (!announcementIds.isEmpty()) {
            List<AnnouncementRead> reads = announcementReadRepository.findByAnnouncementIdsAndUserId(announcementIds, currentUserId);
            for (AnnouncementRead r : reads) {
                readMap.put(r.getAnnouncement().getId(), r);
            }
        }

        Page<AnnouncementResponse> responsePage = announcementPage.map(a -> {
            AnnouncementRead read = readMap.get(a.getId());
            return toAnnouncementResponse(a, read);
        });

        return PageResponse.from(responsePage);
    }

    @Transactional
    public AnnouncementResponse getAnnouncementById(UUID id, UUID currentUserId) {
        User user = getActiveUser(currentUserId);
        Employee employee = user.getEmployee();

        Announcement announcement = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        // Visibility / Authorization guard
        if (!isUserEligibleToViewAnnouncement(announcement, user, employee)) {
            throw new ResourceNotFoundException("Announcement", "id", id);
        }

        // Implicit read tracking on published announcement viewing
        AnnouncementRead read = null;
        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            Optional<AnnouncementRead> existingRead = announcementReadRepository.findByAnnouncementIdAndUserId(id, currentUserId);
            if (existingRead.isPresent()) {
                read = existingRead.get();
            } else {
                read = new AnnouncementRead(announcement, user, Instant.now());
                read = announcementReadRepository.save(read);
            }
        } else {
            read = announcementReadRepository.findByAnnouncementIdAndUserId(id, currentUserId).orElse(null);
        }

        return toAnnouncementResponse(announcement, read);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(UUID id, UpdateAnnouncementRequest request, UUID currentUserId) {
        User user = getActiveUser(currentUserId);

        Announcement announcement = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        // Must be author, Super Admin, or HR Admin
        if (!announcement.getCreatedBy().getId().equals(currentUserId) && !isSuperAdmin(user) && !isHrAdmin(user)) {
            throw new ForbiddenException("You are not authorized to update this announcement");
        }

        // Protect published, cancelled, and archived states
        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw new BadRequestException("Published announcements cannot be modified. Cancel or create a new announcement.");
        }
        if (announcement.getStatus() == AnnouncementStatus.CANCELLED || announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new BadRequestException("Cancelled or archived announcements are immutable.");
        }

        // Re-validate target changes if target parameters were provided
        AnnouncementTargetType targetType = request.targetType() != null ? request.targetType() : announcement.getTargetType();
        Department department = announcement.getDepartment();
        Team team = announcement.getTeam();

        if (request.targetType() != null || request.departmentId() != null || request.teamId() != null) {
            if (targetType == AnnouncementTargetType.COMPANY) {
                if (request.departmentId() != null || request.teamId() != null) {
                    throw new BadRequestException("COMPANY announcements must not specify departmentId or teamId");
                }
                department = null;
                team = null;
            } else if (targetType == AnnouncementTargetType.DEPARTMENT) {
                UUID deptId = request.departmentId() != null ? request.departmentId() : (department != null ? department.getId() : null);
                if (deptId == null) {
                    throw new BadRequestException("departmentId is required for DEPARTMENT announcements");
                }
                department = departmentRepository.findById(deptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Department", "id", deptId));
                team = null;
            } else if (targetType == AnnouncementTargetType.TEAM) {
                UUID tId = request.teamId() != null ? request.teamId() : (team != null ? team.getId() : null);
                if (tId == null) {
                    throw new BadRequestException("teamId is required for TEAM announcements");
                }
                team = teamRepository.findById(tId)
                        .orElseThrow(() -> new ResourceNotFoundException("Team", "id", tId));
                department = team.getDepartment();
            }

            verifyTargetingAuthorization(user, targetType, department, team);
            announcement.setTargetType(targetType);
            announcement.setDepartment(department);
            announcement.setTeam(team);
        }

        if (request.title() != null && !request.title().isBlank()) {
            announcement.setTitle(request.title().trim());
        }
        if (request.content() != null && !request.content().isBlank()) {
            announcement.setContent(request.content().trim());
        }
        if (request.type() != null) {
            announcement.setType(request.type());
        }
        if (request.priority() != null) {
            announcement.setPriority(request.priority());
        }
        if (request.requiresAcknowledgement() != null) {
            announcement.setRequiresAcknowledgement(request.requiresAcknowledgement());
        }

        Instant now = Instant.now();
        if (request.scheduledAt() != null) {
            if (!request.scheduledAt().isAfter(now)) {
                throw new BadRequestException("scheduledAt must be in the future");
            }
            announcement.setScheduledAt(request.scheduledAt());
            announcement.setStatus(AnnouncementStatus.SCHEDULED);
        }

        if (request.expiresAt() != null) {
            if (!request.expiresAt().isAfter(now)) {
                throw new BadRequestException("expiresAt must be in the future");
            }
            announcement.setExpiresAt(request.expiresAt());
        }

        announcement = announcementRepository.save(announcement);

        log.info("Updated announcement: id={}, title='{}', status={}, updatedBy={}",
                announcement.getId(), announcement.getTitle(), announcement.getStatus(), currentUserId);

        auditService.recordAuthEvent(user, AuditAction.ANNOUNCEMENT_UPDATED, "ANNOUNCEMENT", announcement.getId(), null, null,
                buildSanitizedAuditMetadata(announcement));

        return toAnnouncementResponse(announcement, currentUserId);
    }

    @Transactional
    public AnnouncementResponse publishAnnouncement(UUID id, UUID currentUserId) {
        User user = getActiveUser(currentUserId);

        Announcement announcement = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        // Re-validate state
        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw new ConflictException("Announcement is already published");
        }
        if (announcement.getStatus() == AnnouncementStatus.CANCELLED || announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new BadRequestException("Cannot publish an announcement that is " + announcement.getStatus());
        }

        // Re-check targeting authorization at publication time
        verifyTargetingAuthorization(user, announcement.getTargetType(), announcement.getDepartment(), announcement.getTeam());

        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(Instant.now());
        announcement.setPublishedBy(user);

        announcement = announcementRepository.save(announcement);

        log.info("Published announcement: id={}, title='{}', targetType={}, publishedBy={}",
                announcement.getId(), announcement.getTitle(), announcement.getTargetType(), currentUserId);

        auditService.recordAuthEvent(user, AuditAction.ANNOUNCEMENT_PUBLISHED, "ANNOUNCEMENT", announcement.getId(), null, null,
                buildSanitizedAuditMetadata(announcement));

        // Generate notifications for eligible active target employees
        try {
            List<User> targetUsers;
            if (announcement.getTargetType() == AnnouncementTargetType.COMPANY) {
                targetUsers = userRepository.findAllActiveWithEmployees();
            } else if (announcement.getTargetType() == AnnouncementTargetType.DEPARTMENT && announcement.getDepartment() != null) {
                targetUsers = userRepository.findActiveByDepartment(announcement.getDepartment().getId());
            } else if (announcement.getTargetType() == AnnouncementTargetType.TEAM && announcement.getTeam() != null) {
                targetUsers = userRepository.findActiveByTeam(announcement.getTeam().getId());
            } else {
                targetUsers = Collections.emptyList();
            }

            List<UUID> recipientIds = targetUsers.stream().map(User::getId).toList();

            com.logiconnect.platform.notification.entity.NotificationType notifType;
            String notifTitle;
            String notifMessage;

            if (announcement.isRequiresAcknowledgement()) {
                notifType = com.logiconnect.platform.notification.entity.NotificationType.ACKNOWLEDGEMENT_REQUIRED;
                notifTitle = "Action Required: " + announcement.getTitle();
                notifMessage = "Please review and acknowledge the announcement: " + announcement.getTitle();
            } else if (announcement.getPriority() == AnnouncementPriority.URGENT || announcement.getPriority() == AnnouncementPriority.EMERGENCY) {
                notifType = com.logiconnect.platform.notification.entity.NotificationType.URGENT_ANNOUNCEMENT;
                notifTitle = "Urgent: " + announcement.getTitle();
                notifMessage = "Urgent announcement: " + announcement.getTitle();
            } else {
                notifType = com.logiconnect.platform.notification.entity.NotificationType.ANNOUNCEMENT;
                notifTitle = announcement.getTitle();
                notifMessage = "New announcement published: " + announcement.getTitle();
            }

            notificationService.createBulkNotifications(recipientIds, notifType, notifTitle, notifMessage, "ANNOUNCEMENT", announcement.getId());
        } catch (Exception ex) {
            log.error("Failed to generate notifications for announcement: id={}", announcement.getId(), ex);
        }

        return toAnnouncementResponse(announcement, currentUserId);
    }

    @Transactional
    public AnnouncementResponse scheduleAnnouncement(UUID id, ScheduleAnnouncementRequest request, UUID currentUserId) {
        User user = getActiveUser(currentUserId);

        Announcement announcement = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (request.scheduledAt() == null || !request.scheduledAt().isAfter(Instant.now())) {
            throw new BadRequestException("scheduledAt must be in the future");
        }

        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw new ConflictException("Cannot schedule an announcement that is already published");
        }
        if (announcement.getStatus() == AnnouncementStatus.CANCELLED || announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new BadRequestException("Cannot schedule an announcement that is " + announcement.getStatus());
        }

        verifyTargetingAuthorization(user, announcement.getTargetType(), announcement.getDepartment(), announcement.getTeam());

        announcement.setScheduledAt(request.scheduledAt());
        announcement.setStatus(AnnouncementStatus.SCHEDULED);

        announcement = announcementRepository.save(announcement);

        log.info("Scheduled announcement: id={}, scheduledAt={}, scheduledBy={}",
                announcement.getId(), announcement.getScheduledAt(), currentUserId);

        auditService.recordAuthEvent(user, AuditAction.ANNOUNCEMENT_SCHEDULED, "ANNOUNCEMENT", announcement.getId(), null, null,
                buildSanitizedAuditMetadata(announcement));

        return toAnnouncementResponse(announcement, currentUserId);
    }

    @Transactional
    public AnnouncementResponse cancelAnnouncement(UUID id, UUID currentUserId) {
        User user = getActiveUser(currentUserId);

        Announcement announcement = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        // Author or authorized manager/admin can cancel
        if (!announcement.getCreatedBy().getId().equals(currentUserId) && !isSuperAdmin(user) && !isHrAdmin(user)) {
            verifyTargetingAuthorization(user, announcement.getTargetType(), announcement.getDepartment(), announcement.getTeam());
        }

        if (announcement.getStatus() == AnnouncementStatus.CANCELLED) {
            return toAnnouncementResponse(announcement, currentUserId);
        }
        if (announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new BadRequestException("Archived announcements cannot be cancelled");
        }

        announcement.setStatus(AnnouncementStatus.CANCELLED);
        announcement = announcementRepository.save(announcement);

        log.info("Cancelled announcement: id={}, cancelledBy={}", announcement.getId(), currentUserId);

        auditService.recordAuthEvent(user, AuditAction.ANNOUNCEMENT_CANCELLED, "ANNOUNCEMENT", announcement.getId(), null, null,
                buildSanitizedAuditMetadata(announcement));

        return toAnnouncementResponse(announcement, currentUserId);
    }

    @Transactional
    public AnnouncementResponse archiveAnnouncement(UUID id, UUID currentUserId) {
        User user = getActiveUser(currentUserId);

        Announcement announcement = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (!announcement.getCreatedBy().getId().equals(currentUserId) && !isSuperAdmin(user) && !isHrAdmin(user)) {
            verifyTargetingAuthorization(user, announcement.getTargetType(), announcement.getDepartment(), announcement.getTeam());
        }

        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new BadRequestException("Only published announcements can be archived");
        }

        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        announcement = announcementRepository.save(announcement);

        log.info("Archived announcement: id={}, archivedBy={}", announcement.getId(), currentUserId);

        auditService.recordAuthEvent(user, AuditAction.ANNOUNCEMENT_ARCHIVED, "ANNOUNCEMENT", announcement.getId(), null, null,
                buildSanitizedAuditMetadata(announcement));

        return toAnnouncementResponse(announcement, currentUserId);
    }

    @Transactional
    public AnnouncementReadResponse markAsRead(UUID id, UUID currentUserId) {
        User user = getActiveUser(currentUserId);
        Employee employee = user.getEmployee();

        Announcement announcement = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (!isUserEligibleToViewAnnouncement(announcement, user, employee)) {
            throw new ResourceNotFoundException("Announcement", "id", id);
        }

        Optional<AnnouncementRead> existingRead = announcementReadRepository.findByAnnouncementIdAndUserId(id, currentUserId);
        AnnouncementRead readRecord;

        if (existingRead.isPresent()) {
            readRecord = existingRead.get();
            // Preserve original read_at; do not overwrite repeatedly
            if (readRecord.getReadAt() == null) {
                readRecord.setReadAt(Instant.now());
                readRecord = announcementReadRepository.save(readRecord);
            }
        } else {
            readRecord = new AnnouncementRead(announcement, user, Instant.now());
            readRecord = announcementReadRepository.save(readRecord);
        }

        return toAnnouncementReadResponse(readRecord);
    }

    @Transactional
    public AnnouncementReadResponse acknowledgeAnnouncement(UUID id, UUID currentUserId) {
        User user = getActiveUser(currentUserId);
        Employee employee = user.getEmployee();

        Announcement announcement = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new BadRequestException("Only published announcements can be acknowledged");
        }

        if (!announcement.isRequiresAcknowledgement()) {
            throw new BadRequestException("This announcement does not require acknowledgement");
        }

        if (!isUserEligibleToViewAnnouncement(announcement, user, employee)) {
            throw new ResourceNotFoundException("Announcement", "id", id);
        }

        Optional<AnnouncementRead> existingRead = announcementReadRepository.findByAnnouncementIdAndUserId(id, currentUserId);
        AnnouncementRead readRecord;
        Instant now = Instant.now();

        if (existingRead.isPresent()) {
            readRecord = existingRead.get();
            if (readRecord.getReadAt() == null) {
                readRecord.setReadAt(now);
            }
            if (readRecord.getAcknowledgedAt() == null) {
                readRecord.setAcknowledgedAt(now);
            }
            readRecord = announcementReadRepository.save(readRecord);
        } else {
            readRecord = new AnnouncementRead(announcement, user, now);
            readRecord.setAcknowledgedAt(now);
            readRecord = announcementReadRepository.save(readRecord);
        }

        log.info("Announcement acknowledged: id={}, userId={}", id, currentUserId);

        auditService.recordAuthEvent(user, AuditAction.ANNOUNCEMENT_ACKNOWLEDGED, "ANNOUNCEMENT", announcement.getId(), null, null,
                Map.of("announcementId", announcement.getId().toString(), "userId", currentUserId.toString()));

        return toAnnouncementReadResponse(readRecord);
    }

    @Transactional(readOnly = true)
    public AcknowledgementReportResponse getAcknowledgementReport(UUID id, UUID currentUserId) {
        User user = getActiveUser(currentUserId);

        Announcement announcement = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        // Privacy check: ordinary employees must NOT see acknowledgement report
        if (!isUserAuthorizedForReport(announcement, user)) {
            throw new ForbiddenException("You are not authorized to view acknowledgement reports for this announcement");
        }

        // Fetch all eligible employees based on target audience scope
        List<User> eligibleUsers;
        if (announcement.getTargetType() == AnnouncementTargetType.COMPANY) {
            eligibleUsers = userRepository.findAllActiveWithEmployees();
        } else if (announcement.getTargetType() == AnnouncementTargetType.DEPARTMENT && announcement.getDepartment() != null) {
            eligibleUsers = userRepository.findActiveByDepartment(announcement.getDepartment().getId());
        } else if (announcement.getTargetType() == AnnouncementTargetType.TEAM && announcement.getTeam() != null) {
            eligibleUsers = userRepository.findActiveByTeam(announcement.getTeam().getId());
        } else {
            eligibleUsers = Collections.emptyList();
        }

        // Fetch all read records for this announcement
        List<AnnouncementRead> readRecords = announcementReadRepository.findByAnnouncementIdWithUserDetails(id);
        Map<UUID, AnnouncementRead> readMap = readRecords.stream()
                .collect(Collectors.toMap(r -> r.getUser().getId(), r -> r, (r1, r2) -> r1));

        List<EmployeeAcknowledgementStatusDto> statusList = new ArrayList<>();
        long readCount = 0;
        long acknowledgedCount = 0;

        for (User u : eligibleUsers) {
            Employee emp = u.getEmployee();
            AnnouncementRead r = readMap.get(u.getId());

            boolean isRead = r != null && r.getReadAt() != null;
            Instant readAt = r != null ? r.getReadAt() : null;
            boolean isAck = r != null && r.getAcknowledgedAt() != null;
            Instant ackAt = r != null ? r.getAcknowledgedAt() : null;

            if (isRead) readCount++;
            if (isAck) acknowledgedCount++;

            String statusStr = isAck ? "ACKNOWLEDGED" : (isRead ? "READ_PENDING_ACK" : "UNREAD");

            statusList.add(new EmployeeAcknowledgementStatusDto(
                    u.getId(),
                    emp != null ? emp.getId() : null,
                    emp != null ? emp.getEmployeeCode() : null,
                    emp != null ? emp.getFullName() : u.getEmail(),
                    emp != null ? emp.getDesignation() : null,
                    (emp != null && emp.getDepartment() != null) ? emp.getDepartment().getName() : null,
                    (emp != null && emp.getTeam() != null) ? emp.getTeam().getName() : null,
                    isRead,
                    readAt,
                    isAck,
                    ackAt,
                    statusStr
            ));
        }

        long totalEligible = eligibleUsers.size();
        long pendingCount = Math.max(0, totalEligible - acknowledgedCount);
        double ackRate = totalEligible > 0 ? (acknowledgedCount * 100.0 / totalEligible) : 0.0;

        return new AcknowledgementReportResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getTargetType(),
                announcement.isRequiresAcknowledgement(),
                totalEligible,
                readCount,
                acknowledgedCount,
                pendingCount,
                ackRate,
                statusList
        );
    }

    // =========================================================================
    // AUTHORIZATION & HELPER METHODS
    // =========================================================================

    private User getActiveUser(UUID userId) {
        if (userId == null) {
            throw new ForbiddenException("Authentication required");
        }
        User user = userRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("User account is not active");
        }
        if (user.getEmployee() != null && user.getEmployee().getStatus() == EmployeeStatus.TERMINATED) {
            throw new ForbiddenException("Terminated employees cannot access platform services");
        }
        return user;
    }

    private void verifyTargetingAuthorization(User user, AnnouncementTargetType targetType, Department department, Team team) {
        if (isSuperAdmin(user) || isHrAdmin(user)) {
            return;
        }

        Employee emp = user.getEmployee();
        if (emp == null) {
            throw new ForbiddenException("User does not have an active employee profile");
        }

        Set<String> roles = getUserRoleNames(user);

        if (targetType == AnnouncementTargetType.COMPANY) {
            // Broad role check for company broadcast
            if (!roles.contains("SUPER_ADMIN") && !roles.contains("HR_ADMIN") && !hasPermission(user, AppPermission.PUBLISH_ANNOUNCEMENTS) && !hasPermission(user, AppPermission.SYSTEM_ADMIN)) {
                throw new ForbiddenException("You are not authorized to create company-wide announcements");
            }
        } else if (targetType == AnnouncementTargetType.DEPARTMENT) {
            if (department == null) {
                throw new BadRequestException("Department must be specified");
            }
            boolean isDeptManager = (department.getManagerId() != null && department.getManagerId().equals(emp.getId()))
                    || (roles.contains("MANAGER") && emp.getDepartment() != null && emp.getDepartment().getId().equals(department.getId()));

            if (!isDeptManager && !hasPermission(user, AppPermission.MANAGE_DEPARTMENTS) && !hasPermission(user, AppPermission.PUBLISH_ANNOUNCEMENTS)) {
                throw new ForbiddenException("You are not authorized to create announcements for this department");
            }
        } else if (targetType == AnnouncementTargetType.TEAM) {
            if (team == null) {
                throw new BadRequestException("Team must be specified");
            }
            Department parentDept = team.getDepartment();
            boolean isDeptManager = (parentDept != null && parentDept.getManagerId() != null && parentDept.getManagerId().equals(emp.getId()))
                    || (roles.contains("MANAGER") && emp.getDepartment() != null && parentDept != null && emp.getDepartment().getId().equals(parentDept.getId()));

            boolean isTeamLead = (team.getTeamLeadId() != null && team.getTeamLeadId().equals(emp.getId()))
                    || (roles.contains("TEAM_LEADER") && emp.getTeam() != null && emp.getTeam().getId().equals(team.getId()));

            if (!isDeptManager && !isTeamLead && !hasPermission(user, AppPermission.MANAGE_TEAMS) && !hasPermission(user, AppPermission.PUBLISH_ANNOUNCEMENTS)) {
                throw new ForbiddenException("You are not authorized to create announcements for this team");
            }
        }
    }

    private boolean isUserEligibleToViewAnnouncement(Announcement announcement, User user, Employee employee) {
        if (isSuperAdmin(user) || isHrAdmin(user) || announcement.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Drafts, scheduled, cancelled notices are only visible to creators/admins
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED && announcement.getStatus() != AnnouncementStatus.ARCHIVED) {
            return false;
        }

        if (employee == null || employee.getStatus() != EmployeeStatus.ACTIVE) {
            return false;
        }

        if (announcement.getTargetType() == AnnouncementTargetType.COMPANY) {
            return true;
        } else if (announcement.getTargetType() == AnnouncementTargetType.DEPARTMENT) {
            return employee.getDepartment() != null
                    && announcement.getDepartment() != null
                    && employee.getDepartment().getId().equals(announcement.getDepartment().getId());
        } else if (announcement.getTargetType() == AnnouncementTargetType.TEAM) {
            return employee.getTeam() != null
                    && announcement.getTeam() != null
                    && employee.getTeam().getId().equals(announcement.getTeam().getId());
        }
        return false;
    }

    private boolean isUserAuthorizedForReport(Announcement announcement, User user) {
        if (isSuperAdmin(user) || isHrAdmin(user) || announcement.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        Employee emp = user.getEmployee();
        if (emp == null) return false;

        Set<String> roles = getUserRoleNames(user);

        if (announcement.getTargetType() == AnnouncementTargetType.DEPARTMENT && announcement.getDepartment() != null) {
            Department dept = announcement.getDepartment();
            return (dept.getManagerId() != null && dept.getManagerId().equals(emp.getId()))
                    || (roles.contains("MANAGER") && emp.getDepartment() != null && emp.getDepartment().getId().equals(dept.getId()));
        } else if (announcement.getTargetType() == AnnouncementTargetType.TEAM && announcement.getTeam() != null) {
            Team team = announcement.getTeam();
            Department parentDept = team.getDepartment();
            boolean isDeptManager = parentDept != null && (
                    (parentDept.getManagerId() != null && parentDept.getManagerId().equals(emp.getId()))
                            || (roles.contains("MANAGER") && emp.getDepartment() != null && emp.getDepartment().getId().equals(parentDept.getId()))
            );
            boolean isTeamLead = (team.getTeamLeadId() != null && team.getTeamLeadId().equals(emp.getId()))
                    || (roles.contains("TEAM_LEADER") && emp.getTeam() != null && emp.getTeam().getId().equals(team.getId()));
            return isDeptManager || isTeamLead;
        }

        return false;
    }

    private boolean isSuperAdmin(User user) {
        return getUserRoleNames(user).contains("SUPER_ADMIN") || hasPermission(user, AppPermission.SYSTEM_ADMIN);
    }

    private boolean isHrAdmin(User user) {
        return getUserRoleNames(user).contains("HR_ADMIN");
    }

    private Set<String> getUserRoleNames(User user) {
        if (user == null || user.getRoles() == null) return Collections.emptySet();
        return user.getRoles().stream()
                .map(r -> r.getName().replace("ROLE_", "").toUpperCase())
                .collect(Collectors.toSet());
    }

    private boolean hasPermission(User user, String permissionName) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getName().equalsIgnoreCase(permissionName));
    }

    private Map<String, Object> buildSanitizedAuditMetadata(Announcement a) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("title", a.getTitle());
        meta.put("type", a.getType().name());
        meta.put("priority", a.getPriority().name());
        meta.put("targetType", a.getTargetType().name());
        meta.put("status", a.getStatus().name());
        if (a.getDepartment() != null) {
            meta.put("departmentId", a.getDepartment().getId().toString());
        }
        if (a.getTeam() != null) {
            meta.put("teamId", a.getTeam().getId().toString());
        }
        return meta;
    }

    public AnnouncementResponse toAnnouncementResponse(Announcement a, UUID currentUserId) {
        AnnouncementRead read = null;
        if (currentUserId != null) {
            read = announcementReadRepository.findByAnnouncementIdAndUserId(a.getId(), currentUserId).orElse(null);
        }
        return toAnnouncementResponse(a, read);
    }

    public AnnouncementResponse toAnnouncementResponse(Announcement a, AnnouncementRead read) {
        User creator = a.getCreatedBy();
        Employee creatorEmp = creator != null ? creator.getEmployee() : null;
        String creatorName = creatorEmp != null ? creatorEmp.getFullName() : (creator != null ? creator.getEmail() : null);

        User publisher = a.getPublishedBy();
        Employee publisherEmp = publisher != null ? publisher.getEmployee() : null;
        String publisherName = publisherEmp != null ? publisherEmp.getFullName() : (publisher != null ? publisher.getEmail() : null);

        Department dept = a.getDepartment();
        Team team = a.getTeam();

        boolean isRead = read != null && read.getReadAt() != null;
        Instant readAt = read != null ? read.getReadAt() : null;
        boolean isAck = read != null && read.getAcknowledgedAt() != null;
        Instant ackAt = read != null ? read.getAcknowledgedAt() : null;

        return new AnnouncementResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getType(),
                a.getPriority(),
                a.getTargetType(),
                dept != null ? dept.getId() : null,
                dept != null ? dept.getName() : null,
                team != null ? team.getId() : null,
                team != null ? team.getName() : null,
                a.getStatus(),
                a.isRequiresAcknowledgement(),
                a.getScheduledAt(),
                a.getPublishedAt(),
                a.getExpiresAt(),
                creator != null ? creator.getId() : null,
                creatorName,
                publisher != null ? publisher.getId() : null,
                publisherName,
                a.getCreatedAt(),
                a.getUpdatedAt(),
                isRead,
                isAck,
                readAt,
                ackAt
        );
    }

    public AnnouncementReadResponse toAnnouncementReadResponse(AnnouncementRead read) {
        return new AnnouncementReadResponse(
                read.getAnnouncement().getId(),
                read.getUser().getId(),
                read.getReadAt() != null,
                read.getReadAt(),
                read.getAcknowledgedAt() != null,
                read.getAcknowledgedAt()
        );
    }
}
