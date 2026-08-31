package com.logiconnect.platform.conversation.service;

import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.service.AuditService;
import com.logiconnect.platform.common.exception.BadRequestException;
import com.logiconnect.platform.common.exception.ConflictException;
import com.logiconnect.platform.common.exception.ForbiddenException;
import com.logiconnect.platform.common.exception.ResourceNotFoundException;
import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.conversation.dto.*;
import com.logiconnect.platform.conversation.entity.*;
import com.logiconnect.platform.conversation.repository.ConversationMemberRepository;
import com.logiconnect.platform.conversation.repository.ConversationRepository;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.message.entity.Message;
import com.logiconnect.platform.message.repository.MessageRepository;
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
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ConversationResponse createOrGetDirectConversation(CreateDirectConversationRequest request, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (request.targetUserId() == null) {
            throw new BadRequestException("Target user ID is required");
        }
        if (request.targetUserId().equals(currentUserId)) {
            throw new BadRequestException("Cannot create a direct conversation with yourself");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        User targetUser = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.targetUserId()));

        validateUserActiveForMessaging(targetUser);

        // Canonical deterministic locking to prevent race-condition duplicate direct conversations
        UUID first = currentUserId.compareTo(request.targetUserId()) < 0 ? currentUserId : request.targetUserId();
        UUID second = first.equals(currentUserId) ? request.targetUserId() : currentUserId;
        String lockKey = ("DIRECT_CONV:" + first + ":" + second).intern();

        synchronized (lockKey) {
            List<Conversation> existingDirect = conversationRepository.findDirectConversationsBetween(currentUserId, request.targetUserId());
            if (!existingDirect.isEmpty()) {
                log.debug("Found existing direct conversation: id={} between user1={} and user2={}", existingDirect.get(0).getId(), currentUserId, request.targetUserId());
                return toConversationResponse(existingDirect.get(0));
            }

            Conversation conversation = new Conversation();
            conversation.setType(ConversationType.DIRECT);
            conversation.setCreatedBy(currentUser);
            conversation.setArchived(false);

            conversation = conversationRepository.save(conversation);

            ConversationMember member1 = new ConversationMember(conversation, currentUser, ConversationMemberRole.MEMBER);
            ConversationMember member2 = new ConversationMember(conversation, targetUser, ConversationMemberRole.MEMBER);

            conversationMemberRepository.saveAll(List.of(member1, member2));

            log.info("Created direct conversation: id={} between user1={} and user2={}", conversation.getId(), currentUserId, request.targetUserId());
            auditService.recordAuthEvent(currentUser, AuditAction.CONVERSATION_CREATED, "CONVERSATION", conversation.getId(), null, null,
                    Map.of("type", "DIRECT", "targetUserId", request.targetUserId().toString()));

            return toConversationResponse(conversation);
        }
    }

    @Transactional
    public ConversationResponse createGroupConversation(CreateGroupConversationRequest request, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        Set<UUID> memberIdSet = new HashSet<>(request.memberIds() != null ? request.memberIds() : Collections.emptyList());
        memberIdSet.remove(currentUserId); // Creator will be added explicitly as ADMIN

        if (memberIdSet.isEmpty()) {
            throw new BadRequestException("At least one additional member is required to create a group conversation");
        }

        List<User> additionalMembers = new ArrayList<>();
        for (UUID memberId : memberIdSet) {
            User user = userRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", memberId));
            validateUserActiveForMessaging(user);
            additionalMembers.add(user);
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.GROUP);
        conversation.setName(request.name().trim());
        conversation.setDescription(request.description() != null ? request.description().trim() : null);
        conversation.setAvatarUrl(request.avatarUrl() != null ? request.avatarUrl().trim() : null);
        conversation.setCreatedBy(currentUser);
        conversation.setArchived(false);

        conversation = conversationRepository.save(conversation);

        List<ConversationMember> members = new ArrayList<>();
        members.add(new ConversationMember(conversation, currentUser, ConversationMemberRole.ADMIN));

        for (User memberUser : additionalMembers) {
            members.add(new ConversationMember(conversation, memberUser, ConversationMemberRole.MEMBER));
        }

        conversationMemberRepository.saveAll(members);

        log.info("Created group conversation: id={}, name='{}', createdBy={}, memberCount={}", conversation.getId(), conversation.getName(), currentUserId, members.size());
        auditService.recordAuthEvent(currentUser, AuditAction.CONVERSATION_CREATED, "CONVERSATION", conversation.getId(), null, null,
                Map.of("type", "GROUP", "name", conversation.getName(), "memberCount", members.size()));

        return toConversationResponse(conversation);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listMyConversations(ConversationType type, String search, int page, int size, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Order.desc("lastMessageAt").nullsLast(), Sort.Order.desc("createdAt")));

        Page<Conversation> conversationPage = conversationRepository.findUserConversations(currentUserId, type, search, pageable);
        Page<ConversationResponse> responsePage = conversationPage.map(this::toConversationResponse);

        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(UUID id, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", id));

        if (!conversationMemberRepository.isUserActiveMember(id, currentUserId)) {
            throw new ForbiddenException("You are not authorized to access this conversation");
        }

        return toConversationResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationMemberResponse> getConversationMembers(UUID id, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", id));

        if (!conversationMemberRepository.isUserActiveMember(id, currentUserId)) {
            throw new ForbiddenException("You are not authorized to view members of this conversation");
        }

        List<ConversationMember> members = conversationMemberRepository.findActiveMembersByConversationId(id);
        return members.stream().map(this::toConversationMemberResponse).collect(Collectors.toList());
    }

    @Transactional
    public ConversationMemberResponse addMemberToGroup(UUID conversationId, AddConversationMemberRequest request, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (conversation.getType() == ConversationType.DIRECT) {
            throw new BadRequestException("Cannot add members to a direct 1-to-1 conversation");
        }

        ConversationMember callerMember = conversationMemberRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this conversation"));

        if (callerMember.getLeftAt() != null || callerMember.getRole() != ConversationMemberRole.ADMIN) {
            throw new ForbiddenException("Only conversation administrators can add new members");
        }

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.userId()));
        validateUserActiveForMessaging(targetUser);

        Optional<ConversationMember> existingMemberOpt = conversationMemberRepository.findByConversationIdAndUserId(conversationId, request.userId());
        ConversationMember member;

        if (existingMemberOpt.isPresent()) {
            member = existingMemberOpt.get();
            if (member.getLeftAt() == null) {
                throw new ConflictException("User is already an active member of this conversation");
            }
            // Re-joining group
            member.setLeftAt(null);
            member.setJoinedAt(Instant.now());
            if (request.role() != null) {
                member.setRole(request.role());
            }
        } else {
            ConversationMemberRole role = request.role() != null ? request.role() : ConversationMemberRole.MEMBER;
            member = new ConversationMember(conversation, targetUser, role);
        }

        member = conversationMemberRepository.save(member);

        log.info("Added member userId={} to conversationId={} with role={}", targetUser.getId(), conversationId, member.getRole());
        auditService.recordAuthEvent(callerMember.getUser(), AuditAction.GROUP_MEMBER_ADDED, "CONVERSATION_MEMBER", targetUser.getId(), null, null,
                Map.of("conversationId", conversationId.toString(), "targetUserId", targetUser.getId().toString(), "role", member.getRole().name()));

        return toConversationMemberResponse(member);
    }

    @Transactional
    public void removeMemberFromGroup(UUID conversationId, UUID targetUserId, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (conversation.getType() == ConversationType.DIRECT) {
            throw new BadRequestException("Cannot remove members from a direct 1-to-1 conversation");
        }

        ConversationMember targetMember = conversationMemberRepository.findByConversationIdAndUserId(conversationId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "userId", targetUserId));

        if (targetMember.getLeftAt() != null) {
            throw new BadRequestException("User is not an active member of this conversation");
        }

        boolean isSelfLeaving = targetUserId.equals(currentUserId);

        if (!isSelfLeaving) {
            ConversationMember callerMember = conversationMemberRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                    .orElseThrow(() -> new ForbiddenException("You are not a member of this conversation"));

            if (callerMember.getLeftAt() != null || callerMember.getRole() != ConversationMemberRole.ADMIN) {
                throw new ForbiddenException("Only conversation administrators can remove other members");
            }
        }

        // Last-Admin Protection
        if (targetMember.getRole() == ConversationMemberRole.ADMIN) {
            long activeAdmins = conversationMemberRepository.countActiveMembersByRole(conversationId, ConversationMemberRole.ADMIN);
            long activeMembers = conversationMemberRepository.countActiveMembersByConversationId(conversationId);

            if (activeAdmins <= 1 && activeMembers > 1) {
                throw new BadRequestException("Cannot remove or leave as the only group administrator while other members exist. Please promote another administrator first.");
            }
        }

        targetMember.setLeftAt(Instant.now());
        conversationMemberRepository.save(targetMember);

        log.info("Removed member userId={} from conversationId={}", targetUserId, conversationId);
        auditService.recordAuthEvent(targetMember.getUser(), AuditAction.GROUP_MEMBER_REMOVED, "CONVERSATION_MEMBER", targetUserId, null, null,
                Map.of("conversationId", conversationId.toString(), "removedUserId", targetUserId.toString(), "selfLeft", isSelfLeaving));
    }

    private void validateUserActiveForMessaging(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User account is not active");
        }
        Employee employee = user.getEmployee();
        if (employee != null && !employee.getStatus().isEligibleForLogin()) {
            throw new BadRequestException("Employee status does not permit messaging access");
        }
    }

    public ConversationResponse toConversationResponse(Conversation conversation) {
        List<ConversationMember> activeMembers = conversationMemberRepository.findActiveMembersByConversationId(conversation.getId());
        List<ConversationMemberResponse> memberResponses = activeMembers.stream()
                .map(this::toConversationMemberResponse)
                .collect(Collectors.toList());

        LastMessageResponse lastMessageResponse = null;
        Optional<Message> latestMsgOpt = messageRepository.findLatestMessageByConversationId(conversation.getId());
        if (latestMsgOpt.isPresent()) {
            Message msg = latestMsgOpt.get();
            String content = msg.isDeleted() ? null : msg.getContent();
            String senderName = msg.getSender() != null && msg.getSender().getEmployee() != null
                    ? msg.getSender().getEmployee().getFullName()
                    : null;

            lastMessageResponse = new LastMessageResponse(
                    msg.getId(),
                    msg.getSender() != null ? msg.getSender().getId() : null,
                    senderName,
                    content,
                    msg.getMessageType(),
                    msg.getCreatedAt()
            );
        }

        String createdByName = conversation.getCreatedBy() != null && conversation.getCreatedBy().getEmployee() != null
                ? conversation.getCreatedBy().getEmployee().getFullName()
                : null;

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getDescription(),
                conversation.getAvatarUrl(),
                conversation.getCreatedBy() != null ? conversation.getCreatedBy().getId() : null,
                createdByName,
                conversation.isArchived(),
                activeMembers.size(),
                memberResponses,
                lastMessageResponse,
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public ConversationMemberResponse toConversationMemberResponse(ConversationMember member) {
        User user = member.getUser();
        Employee emp = user != null ? user.getEmployee() : null;

        return new ConversationMemberResponse(
                user != null ? user.getId() : null,
                emp != null ? emp.getEmployeeCode() : null,
                emp != null ? emp.getFullName() : null,
                emp != null ? emp.getFirstName() : null,
                emp != null ? emp.getLastName() : null,
                user != null ? user.getEmail() : null,
                emp != null ? emp.getProfilePhotoUrl() : null,
                emp != null ? emp.getDesignation() : null,
                member.getRole(),
                member.getJoinedAt(),
                member.isMuted(),
                member.isPinned()
        );
    }
}
