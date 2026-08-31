package com.logiconnect.platform.channel.service;

import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.service.AuditService;
import com.logiconnect.platform.channel.dto.*;
import com.logiconnect.platform.channel.entity.*;
import com.logiconnect.platform.channel.repository.ChannelMemberRepository;
import com.logiconnect.platform.channel.repository.ChannelRepository;
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
import com.logiconnect.platform.role.entity.Role;
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

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ChannelService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ChannelResponse createChannel(CreateChannelRequest request, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // 1. RBAC authorization for channel creation
        verifyChannelCreationPermission(creator, request.type(), request.departmentId(), request.teamId());

        Department department = null;
        Team team = null;

        if (request.type() == ChannelType.DEPARTMENT) {
            if (request.departmentId() == null) {
                throw new BadRequestException("departmentId is required for DEPARTMENT channels");
            }
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
        } else if (request.type() == ChannelType.TEAM) {
            if (request.teamId() == null) {
                throw new BadRequestException("teamId is required for TEAM channels");
            }
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.teamId()));
            department = team.getDepartment();
        } else if (request.type() == ChannelType.COMPANY) {
            if (request.departmentId() != null || request.teamId() != null) {
                throw new BadRequestException("COMPANY channels must not be associated with a specific department or team");
            }
        }

        // Generate unique slug
        String baseSlug = (request.slug() != null && !request.slug().isBlank())
                ? toSlug(request.slug())
                : toSlug(request.name());

        String slug = baseSlug;
        int counter = 1;
        while (channelRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Channel channel = new Channel();
        channel.setName(request.name().trim());
        channel.setSlug(slug);
        channel.setDescription(request.description() != null ? request.description().trim() : null);
        channel.setType(request.type());
        channel.setDepartment(department);
        channel.setTeam(team);
        channel.setCreatedBy(creator);
        channel.setStatus(ChannelStatus.ACTIVE);
        channel.setReadOnly(request.isReadOnly() != null ? request.isReadOnly() : false);

        channel = channelRepository.save(channel);

        // Creator automatically becomes Channel ADMIN
        ChannelMember creatorMember = new ChannelMember(channel, creator, ChannelMemberRole.ADMIN);
        channelMemberRepository.save(creatorMember);

        log.info("Created channel: id={}, name='{}', slug='{}', type={}, createdBy={}",
                channel.getId(), channel.getName(), channel.getSlug(), channel.getType(), currentUserId);

        auditService.recordAuthEvent(creator, AuditAction.CHANNEL_CREATED, "CHANNEL", channel.getId(), null, null,
                Map.of("name", channel.getName(), "type", channel.getType().name(), "slug", channel.getSlug()));

        return toChannelResponse(channel, currentUserId, List.of(toChannelMemberResponse(creatorMember)));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChannelResponse> listChannels(ChannelType type, ChannelStatus status, String search, int page, int size, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.ASC, "name"));
        ChannelStatus targetStatus = status != null ? status : ChannelStatus.ACTIVE;
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Channel> channelPage;
        if (isSuperAdmin(user)) {
            channelPage = channelRepository.findAllAdmin(type, targetStatus, searchTerm, pageable);
        } else {
            Employee employee = user.getEmployee();
            UUID deptId = (employee != null && employee.getDepartment() != null) ? employee.getDepartment().getId() : null;
            UUID teamId = (employee != null && employee.getTeam() != null) ? employee.getTeam().getId() : null;

            channelPage = channelRepository.findAccessibleChannels(currentUserId, deptId, teamId, type, targetStatus, searchTerm, pageable);
        }

        Page<ChannelResponse> responsePage = channelPage.map(c -> toChannelResponse(c, currentUserId, null));
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public ChannelResponse getChannelById(UUID id, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", id));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!isUserAuthorizedForChannel(channel, user)) {
            throw new ForbiddenException("You are not authorized to view this channel");
        }

        List<ChannelMemberResponse> members = channelMemberRepository.findByChannelIdWithDetails(id).stream()
                .map(this::toChannelMemberResponse)
                .collect(Collectors.toList());

        return toChannelResponse(channel, currentUserId, members);
    }

    @Transactional
    public ChannelResponse updateChannel(UUID id, UpdateChannelRequest request, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", id));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!isChannelAdminOrSuperAdmin(channel, user)) {
            throw new ForbiddenException("Only channel administrators can update channel settings");
        }

        if (request.name() != null && !request.name().isBlank()) {
            channel.setName(request.name().trim());
        }
        if (request.description() != null) {
            channel.setDescription(request.description().trim());
        }
        if (request.isReadOnly() != null) {
            channel.setReadOnly(request.isReadOnly());
        }
        if (request.status() != null && request.status() != channel.getStatus()) {
            channel.setStatus(request.status());
            if (request.status() == ChannelStatus.ARCHIVED) {
                auditService.recordAuthEvent(user, AuditAction.CHANNEL_ARCHIVED, "CHANNEL", channel.getId(), null, null,
                        Map.of("name", channel.getName()));
            }
        }

        channel = channelRepository.save(channel);

        log.info("Updated channel: id={}, updatedBy={}", id, currentUserId);
        auditService.recordAuthEvent(user, AuditAction.CHANNEL_UPDATED, "CHANNEL", channel.getId(), null, null,
                Map.of("name", channel.getName()));

        return toChannelResponse(channel, currentUserId, null);
    }

    @Transactional
    public ChannelMemberResponse joinChannel(UUID id, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", id));

        if (channel.getStatus() != ChannelStatus.ACTIVE) {
            throw new BadRequestException("Cannot join an archived or inactive channel");
        }

        if (channel.getType() == ChannelType.PRIVATE) {
            throw new ForbiddenException("Cannot self-join a private channel. Membership requires an administrator invitation.");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        Employee emp = user.getEmployee();
        if (emp != null && emp.getStatus() != EmployeeStatus.ACTIVE) {
            throw new ForbiddenException("Only active employees may join channels");
        }

        // Validate organizational eligibility
        if (channel.getType() == ChannelType.DEPARTMENT) {
            if (emp == null || emp.getDepartment() == null || !emp.getDepartment().getId().equals(channel.getDepartment().getId())) {
                throw new ForbiddenException("You are not eligible to join this department channel");
            }
        } else if (channel.getType() == ChannelType.TEAM) {
            if (emp == null || emp.getTeam() == null || !emp.getTeam().getId().equals(channel.getTeam().getId())) {
                throw new ForbiddenException("You are not eligible to join this team channel");
            }
        }

        if (channelMemberRepository.existsByChannelIdAndUserId(id, currentUserId)) {
            throw new ConflictException("User is already a member of this channel");
        }

        ChannelMember member = new ChannelMember(channel, user, ChannelMemberRole.MEMBER);
        member = channelMemberRepository.save(member);

        log.info("User {} joined channel {}", currentUserId, id);
        auditService.recordAuthEvent(user, AuditAction.CHANNEL_MEMBER_ADDED, "CHANNEL_MEMBER", user.getId(), null, null,
                Map.of("channelId", id.toString(), "action", "SELF_JOIN"));

        return toChannelMemberResponse(member);
    }

    @Transactional
    public ChannelMemberResponse addMember(UUID id, AddChannelMemberRequest request, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", id));

        User caller = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!isChannelAdminOrSuperAdmin(channel, caller)) {
            throw new ForbiddenException("Only channel administrators can add new members");
        }

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.userId()));

        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Cannot add an inactive user to a channel");
        }

        if (channelMemberRepository.existsByChannelIdAndUserId(id, targetUser.getId())) {
            throw new ConflictException("User is already a member of this channel");
        }

        ChannelMemberRole role = request.role() != null ? request.role() : ChannelMemberRole.MEMBER;
        ChannelMember member = new ChannelMember(channel, targetUser, role);
        member = channelMemberRepository.save(member);

        log.info("Added user {} to channel {} with role {} by admin {}", targetUser.getId(), id, role, currentUserId);
        auditService.recordAuthEvent(caller, AuditAction.CHANNEL_MEMBER_ADDED, "CHANNEL_MEMBER", targetUser.getId(), null, null,
                Map.of("channelId", id.toString(), "role", role.name(), "addedBy", currentUserId.toString()));

        return toChannelMemberResponse(member);
    }

    @Transactional
    public void removeMember(UUID id, UUID targetUserId, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", id));

        User caller = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isSelfLeaving = currentUserId.equals(targetUserId);

        if (!isSelfLeaving && !isChannelAdminOrSuperAdmin(channel, caller)) {
            throw new ForbiddenException("Only channel administrators can remove members");
        }

        if (!channelMemberRepository.existsByChannelIdAndUserId(id, targetUserId)) {
            throw new ResourceNotFoundException("Channel membership", "userId", targetUserId);
        }

        channelMemberRepository.deleteByChannelIdAndUserId(id, targetUserId);

        log.info("Removed user {} from channel {} by {}", targetUserId, id, currentUserId);
        auditService.recordAuthEvent(caller, AuditAction.CHANNEL_MEMBER_REMOVED, "CHANNEL_MEMBER", targetUserId, null, null,
                Map.of("channelId", id.toString(), "removedBy", currentUserId.toString()));
    }

    @Transactional(readOnly = true)
    public List<ChannelMemberResponse> getChannelMembers(UUID id, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", id));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!isUserAuthorizedForChannel(channel, user)) {
            throw new ForbiddenException("You are not authorized to view members of this channel");
        }

        return channelMemberRepository.findByChannelIdWithDetails(id).stream()
                .map(this::toChannelMemberResponse)
                .collect(Collectors.toList());
    }

    public boolean isUserAuthorizedForChannel(Channel channel, User user) {
        if (user == null) return false;
        if (isSuperAdmin(user)) return true;

        // Explicit membership always grants access
        if (channelMemberRepository.existsByChannelIdAndUserId(channel.getId(), user.getId())) {
            return true;
        }

        // Private channels require explicit membership
        if (channel.getType() == ChannelType.PRIVATE) {
            return false;
        }

        Employee employee = user.getEmployee();
        if (employee == null || employee.getStatus() != EmployeeStatus.ACTIVE) {
            return false;
        }

        if (channel.getType() == ChannelType.COMPANY) {
            return true;
        }

        if (channel.getType() == ChannelType.DEPARTMENT) {
            return employee.getDepartment() != null &&
                    channel.getDepartment() != null &&
                    employee.getDepartment().getId().equals(channel.getDepartment().getId());
        }

        if (channel.getType() == ChannelType.TEAM) {
            return employee.getTeam() != null &&
                    channel.getTeam() != null &&
                    employee.getTeam().getId().equals(channel.getTeam().getId());
        }

        return false;
    }

    public boolean isChannelAdminOrModerator(Channel channel, User user) {
        if (user == null) return false;
        if (isSuperAdmin(user)) return true;

        Optional<ChannelMember> memberOpt = channelMemberRepository.findByChannelIdAndUserId(channel.getId(), user.getId());
        return memberOpt.isPresent() &&
                (memberOpt.get().getRole() == ChannelMemberRole.ADMIN || memberOpt.get().getRole() == ChannelMemberRole.MODERATOR);
    }

    public boolean isChannelAdminOrSuperAdmin(Channel channel, User user) {
        if (user == null) return false;
        if (isSuperAdmin(user)) return true;

        Optional<ChannelMember> memberOpt = channelMemberRepository.findByChannelIdAndUserId(channel.getId(), user.getId());
        return memberOpt.isPresent() && memberOpt.get().getRole() == ChannelMemberRole.ADMIN;
    }

    private void verifyChannelCreationPermission(User creator, ChannelType type, UUID departmentId, UUID teamId) {
        Set<String> roleNames = creator.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        boolean hasSuperAdmin = roleNames.contains("SUPER_ADMIN") || hasPermission(creator, AppPermission.SYSTEM_ADMIN);
        boolean hasHrAdmin = roleNames.contains("HR_ADMIN");
        boolean hasManager = roleNames.contains("MANAGER");
        boolean hasTeamLeader = roleNames.contains("TEAM_LEADER");
        boolean hasManageChannels = hasPermission(creator, AppPermission.MANAGE_CHANNELS);

        if (hasSuperAdmin || hasManageChannels) {
            return; // Can create any channel
        }

        Employee emp = creator.getEmployee();

        if (type == ChannelType.COMPANY) {
            if (!hasHrAdmin) {
                throw new ForbiddenException("Only administrators can create company-wide channels");
            }
        } else if (type == ChannelType.DEPARTMENT) {
            if (hasHrAdmin) return;
            if (hasManager && emp != null && emp.getDepartment() != null && emp.getDepartment().getId().equals(departmentId)) {
                return;
            }
            throw new ForbiddenException("You are not authorized to create a department channel for this department");
        } else if (type == ChannelType.TEAM) {
            if (hasHrAdmin) return;
            if (hasManager && emp != null && emp.getDepartment() != null) {
                Team targetTeam = teamRepository.findById(teamId).orElse(null);
                if (targetTeam != null && targetTeam.getDepartment() != null && targetTeam.getDepartment().getId().equals(emp.getDepartment().getId())) {
                    return;
                }
            }
            if (hasTeamLeader && emp != null && emp.getTeam() != null && emp.getTeam().getId().equals(teamId)) {
                return;
            }
            throw new ForbiddenException("You are not authorized to create a team channel for this team");
        } else if (type == ChannelType.PRIVATE) {
            if (hasHrAdmin || hasManager || hasTeamLeader) {
                return;
            }
            throw new ForbiddenException("You do not have permission to create organizational channels");
        } else {
            throw new ForbiddenException("Unauthorized to create channels of type " + type);
        }
    }

    private boolean isSuperAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getName())) ||
                hasPermission(user, AppPermission.SYSTEM_ADMIN);
    }

    private boolean hasPermission(User user, String permissionName) {
        return user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getName().equalsIgnoreCase(permissionName));
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input.trim().toLowerCase()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    public ChannelResponse toChannelResponse(Channel channel, UUID currentUserId, List<ChannelMemberResponse> members) {
        long memberCount = channelMemberRepository.countByChannelId(channel.getId());

        boolean isMember = false;
        ChannelMemberRole userRole = null;
        if (currentUserId != null) {
            Optional<ChannelMember> memberOpt = channelMemberRepository.findByChannelIdAndUserId(channel.getId(), currentUserId);
            if (memberOpt.isPresent()) {
                isMember = true;
                userRole = memberOpt.get().getRole();
            }
        }

        User creator = channel.getCreatedBy();
        Employee creatorEmp = creator != null ? creator.getEmployee() : null;
        String creatorName = creatorEmp != null ? creatorEmp.getFullName() : (creator != null ? creator.getEmail() : null);

        Department dept = channel.getDepartment();
        Team team = channel.getTeam();

        return new ChannelResponse(
                channel.getId(),
                channel.getName(),
                channel.getSlug(),
                channel.getDescription(),
                channel.getType(),
                dept != null ? dept.getId() : null,
                dept != null ? dept.getName() : null,
                team != null ? team.getId() : null,
                team != null ? team.getName() : null,
                creator != null ? creator.getId() : null,
                creatorName,
                channel.getStatus(),
                channel.isReadOnly(),
                memberCount,
                isMember,
                userRole,
                channel.getCreatedAt(),
                channel.getUpdatedAt(),
                members
        );
    }

    public ChannelMemberResponse toChannelMemberResponse(ChannelMember member) {
        User u = member.getUser();
        Employee e = u != null ? u.getEmployee() : null;

        return new ChannelMemberResponse(
                member.getChannel().getId(),
                u != null ? u.getId() : null,
                e != null ? e.getEmployeeCode() : null,
                e != null ? e.getFullName() : null,
                e != null ? e.getFirstName() : null,
                e != null ? e.getLastName() : null,
                e != null ? e.getProfilePhotoUrl() : null,
                e != null ? e.getDesignation() : null,
                e != null && e.getDepartment() != null ? e.getDepartment().getName() : null,
                member.getRole(),
                member.getJoinedAt(),
                member.isMuted()
        );
    }
}
