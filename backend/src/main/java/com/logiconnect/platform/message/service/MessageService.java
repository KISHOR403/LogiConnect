package com.logiconnect.platform.message.service;

import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.service.AuditService;
import com.logiconnect.platform.channel.entity.Channel;
import com.logiconnect.platform.channel.entity.ChannelMemberRole;
import com.logiconnect.platform.channel.entity.ChannelStatus;
import com.logiconnect.platform.channel.repository.ChannelMemberRepository;
import com.logiconnect.platform.channel.repository.ChannelRepository;
import com.logiconnect.platform.channel.service.ChannelService;
import com.logiconnect.platform.common.exception.BadRequestException;
import com.logiconnect.platform.common.exception.ForbiddenException;
import com.logiconnect.platform.common.exception.ResourceNotFoundException;
import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.conversation.entity.Conversation;
import com.logiconnect.platform.conversation.repository.ConversationMemberRepository;
import com.logiconnect.platform.conversation.repository.ConversationRepository;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.message.dto.*;
import com.logiconnect.platform.message.entity.Message;
import com.logiconnect.platform.message.entity.MessageAttachment;
import com.logiconnect.platform.message.entity.MessageType;
import com.logiconnect.platform.message.repository.MessageAttachmentRepository;
import com.logiconnect.platform.message.repository.MessageRepository;
import com.logiconnect.platform.user.entity.User;
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
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_CONTENT_LENGTH = 10000;

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelService channelService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public MessageService(
            MessageRepository messageRepository,
            MessageAttachmentRepository attachmentRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelService channelService,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelService = channelService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ==========================================
    // 1. CONVERSATION MESSAGES
    // ==========================================

    @Transactional
    public MessageResponse sendMessage(UUID conversationId, SendMessageRequest request, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (conversation.isArchived()) {
            throw new BadRequestException("Cannot post messages to an archived conversation");
        }

        if (!conversationMemberRepository.isUserActiveMember(conversationId, currentUserId)) {
            throw new ForbiddenException("You are not an active member of this conversation");
        }

        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // Validate content and attachments
        boolean hasAttachments = request.attachments() != null && !request.attachments().isEmpty();
        String content = request.content() != null ? request.content().trim() : null;

        if ((content == null || content.isEmpty()) && !hasAttachments) {
            throw new BadRequestException("Message must contain either text content or an attachment");
        }

        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("Message content exceeds maximum allowed limit of " + MAX_CONTENT_LENGTH + " characters");
        }

        // Validate reply parent if specified
        Message parentMessage = null;
        if (request.replyToMessageId() != null) {
            parentMessage = messageRepository.findById(request.replyToMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reply parent message", "id", request.replyToMessageId()));

            if (parentMessage.getConversation() == null || !parentMessage.getConversation().getId().equals(conversationId)) {
                throw new BadRequestException("Replied message does not belong to the same conversation");
            }
        }

        MessageType type = request.messageType() != null ? request.messageType() : (hasAttachments ? MessageType.FILE : MessageType.TEXT);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessageType(type);
        message.setContent(content);
        message.setReplyToMessage(parentMessage);

        message = messageRepository.save(message);

        if (hasAttachments) {
            List<MessageAttachment> attachments = new ArrayList<>();
            for (AttachmentRequest attReq : request.attachments()) {
                MessageAttachment attachment = new MessageAttachment(
                        message,
                        attReq.storageKey().trim(),
                        attReq.fileUrl().trim(),
                        attReq.fileName().trim(),
                        attReq.fileType().trim(),
                        attReq.fileSize()
                );
                attachments.add(attachment);
            }
            attachmentRepository.saveAll(attachments);
            message.setAttachments(attachments);
        }

        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        log.info("Message sent: id={} in conversationId={} by senderId={}", message.getId(), conversationId, currentUserId);
        return toMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(UUID conversationId, int page, int size, String direction, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        if (!conversationMemberRepository.isUserActiveMember(conversationId, currentUserId)) {
            throw new ForbiddenException("You are not an active member of this conversation");
        }

        size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(sortDirection, "createdAt", "id"));

        Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageable);
        Page<MessageResponse> responsePage = messagePage.map(this::toMessageResponse);

        return PageResponse.from(responsePage);
    }

    // ==========================================
    // 2. CHANNEL MESSAGES
    // ==========================================

    @Transactional
    public MessageResponse sendChannelMessage(UUID channelId, SendMessageRequest request, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        if (channel.getStatus() != ChannelStatus.ACTIVE) {
            throw new BadRequestException("Cannot post messages to an archived or inactive channel");
        }

        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!channelService.isUserAuthorizedForChannel(channel, sender)) {
            throw new ForbiddenException("You are not authorized to send messages in this channel");
        }

        if (channel.isReadOnly() && !channelService.isChannelAdminOrModerator(channel, sender)) {
            throw new ForbiddenException("This channel is read-only. Only channel moderators or administrators can post messages.");
        }

        boolean hasAttachments = request.attachments() != null && !request.attachments().isEmpty();
        String content = request.content() != null ? request.content().trim() : null;

        if ((content == null || content.isEmpty()) && !hasAttachments) {
            throw new BadRequestException("Message must contain either text content or an attachment");
        }

        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("Message content exceeds maximum allowed limit of " + MAX_CONTENT_LENGTH + " characters");
        }

        Message parentMessage = null;
        if (request.replyToMessageId() != null) {
            parentMessage = messageRepository.findById(request.replyToMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reply parent message", "id", request.replyToMessageId()));

            if (parentMessage.getChannel() == null || !parentMessage.getChannel().getId().equals(channelId)) {
                throw new BadRequestException("Replied message does not belong to the same channel");
            }
        }

        MessageType type = request.messageType() != null ? request.messageType() : (hasAttachments ? MessageType.FILE : MessageType.TEXT);

        Message message = new Message();
        message.setChannel(channel);
        message.setSender(sender);
        message.setMessageType(type);
        message.setContent(content);
        message.setReplyToMessage(parentMessage);

        message = messageRepository.save(message);

        if (hasAttachments) {
            List<MessageAttachment> attachments = new ArrayList<>();
            for (AttachmentRequest attReq : request.attachments()) {
                MessageAttachment attachment = new MessageAttachment(
                        message,
                        attReq.storageKey().trim(),
                        attReq.fileUrl().trim(),
                        attReq.fileName().trim(),
                        attReq.fileType().trim(),
                        attReq.fileSize()
                );
                attachments.add(attachment);
            }
            attachmentRepository.saveAll(attachments);
            message.setAttachments(attachments);
        }

        log.info("Channel message sent: id={} in channelId={} by senderId={}", message.getId(), channelId, currentUserId);
        return toMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listChannelMessages(UUID channelId, int page, int size, String direction, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!channelService.isUserAuthorizedForChannel(channel, user)) {
            throw new ForbiddenException("You are not authorized to view messages in this channel");
        }

        size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(sortDirection, "createdAt", "id"));

        Page<Message> messagePage = messageRepository.findByChannelId(channelId, pageable);
        Page<MessageResponse> responsePage = messagePage.map(this::toMessageResponse);

        return PageResponse.from(responsePage);
    }

    @Transactional
    public MessageResponse pinChannelMessage(UUID channelId, UUID messageId, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        User caller = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!channelService.isChannelAdminOrModerator(channel, caller)) {
            throw new ForbiddenException("Only channel administrators or moderators can pin messages");
        }

        Message message = messageRepository.findByIdWithDetails(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (message.getChannel() == null || !message.getChannel().getId().equals(channelId)) {
            throw new BadRequestException("Message does not belong to the specified channel");
        }

        message.setPinned(true);
        message = messageRepository.save(message);

        log.info("Message pinned: id={} in channelId={} by user={}", messageId, channelId, currentUserId);
        auditService.recordAuthEvent(caller, AuditAction.CHANNEL_MESSAGE_PINNED, "MESSAGE", messageId, null, null,
                Map.of("channelId", channelId.toString()));

        return toMessageResponse(message);
    }

    @Transactional
    public MessageResponse unpinChannelMessage(UUID channelId, UUID messageId, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId));

        User caller = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!channelService.isChannelAdminOrModerator(channel, caller)) {
            throw new ForbiddenException("Only channel administrators or moderators can unpin messages");
        }

        Message message = messageRepository.findByIdWithDetails(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (message.getChannel() == null || !message.getChannel().getId().equals(channelId)) {
            throw new BadRequestException("Message does not belong to the specified channel");
        }

        message.setPinned(false);
        message = messageRepository.save(message);

        log.info("Message unpinned: id={} in channelId={} by user={}", messageId, channelId, currentUserId);
        auditService.recordAuthEvent(caller, AuditAction.CHANNEL_MESSAGE_UNPINNED, "MESSAGE", messageId, null, null,
                Map.of("channelId", channelId.toString()));

        return toMessageResponse(message);
    }

    // ==========================================
    // 3. COMMON MESSAGE OPERATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public MessageResponse getMessageById(UUID id, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Message message = messageRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", id));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (message.getConversation() != null) {
            if (!conversationMemberRepository.isUserActiveMember(message.getConversation().getId(), currentUserId)) {
                throw new ForbiddenException("You are not authorized to view this message");
            }
        } else if (message.getChannel() != null) {
            if (!channelService.isUserAuthorizedForChannel(message.getChannel(), user)) {
                throw new ForbiddenException("You are not authorized to view this message");
            }
        }

        return toMessageResponse(message);
    }

    @Transactional
    public MessageResponse editMessage(UUID id, EditMessageRequest request, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Message message = messageRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", id));

        if (message.getSender() == null || !message.getSender().getId().equals(currentUserId)) {
            throw new ForbiddenException("You can only edit your own messages");
        }

        if (message.isDeleted()) {
            throw new BadRequestException("Cannot edit a deleted message");
        }

        if (message.getConversation() != null && !conversationMemberRepository.isUserActiveMember(message.getConversation().getId(), currentUserId)) {
            throw new ForbiddenException("You are no longer an active member of this conversation");
        }

        String content = request.content().trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("Message content exceeds maximum allowed limit of " + MAX_CONTENT_LENGTH + " characters");
        }

        message.setContent(content);
        message.setEditedAt(Instant.now());
        message = messageRepository.save(message);

        log.info("Message edited: id={} by senderId={}", id, currentUserId);
        Map<String, Object> auditMeta = new HashMap<>();
        if (message.getConversation() != null) {
            auditMeta.put("conversationId", message.getConversation().getId().toString());
        }
        if (message.getChannel() != null) {
            auditMeta.put("channelId", message.getChannel().getId().toString());
        }
        auditService.recordAuthEvent(message.getSender(), AuditAction.MESSAGE_EDITED, "MESSAGE", id, null, null, auditMeta);

        return toMessageResponse(message);
    }

    @Transactional
    public MessageResponse softDeleteMessage(UUID id, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }

        Message message = messageRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", id));

        User caller = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isSender = message.getSender() != null && message.getSender().getId().equals(currentUserId);
        boolean isModerator = false;

        if (message.getChannel() != null) {
            isModerator = channelService.isChannelAdminOrModerator(message.getChannel(), caller);
        }

        if (!isSender && !isModerator) {
            throw new ForbiddenException("You are not authorized to delete this message");
        }

        if (!message.isDeleted()) {
            message.setDeletedAt(Instant.now());
            message = messageRepository.save(message);

            log.info("Message soft-deleted: id={} by userId={}, wasModerator={}", id, currentUserId, isModerator && !isSender);
            Map<String, Object> auditMeta = new HashMap<>();
            if (message.getConversation() != null) {
                auditMeta.put("conversationId", message.getConversation().getId().toString());
            }
            if (message.getChannel() != null) {
                auditMeta.put("channelId", message.getChannel().getId().toString());
            }
            auditMeta.put("deletedBy", currentUserId.toString());
            if (isModerator && !isSender) {
                auditMeta.put("action", "MODERATION_DELETE");
            }
            auditService.recordAuthEvent(caller, AuditAction.MESSAGE_DELETED, "MESSAGE", id, null, null, auditMeta);
        }

        return toMessageResponse(message);
    }

    public MessageResponse toMessageResponse(Message message) {
        User senderUser = message.getSender();
        Employee senderEmp = senderUser != null ? senderUser.getEmployee() : null;

        MessageSenderResponse sender = new MessageSenderResponse(
                senderUser != null ? senderUser.getId() : null,
                senderEmp != null ? senderEmp.getEmployeeCode() : null,
                senderEmp != null ? senderEmp.getFullName() : null,
                senderEmp != null ? senderEmp.getFirstName() : null,
                senderEmp != null ? senderEmp.getLastName() : null,
                senderEmp != null ? senderEmp.getProfilePhotoUrl() : null,
                senderEmp != null ? senderEmp.getDesignation() : null
        );

        List<AttachmentResponse> attachmentResponses = Collections.emptyList();
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            attachmentResponses = message.getAttachments().stream()
                    .map(att -> new AttachmentResponse(
                            att.getId(),
                            att.getStorageKey(),
                            att.getFileUrl(),
                            att.getFileName(),
                            att.getFileType(),
                            att.getFileSize(),
                            att.getCreatedAt()
                    ))
                    .collect(Collectors.toList());
        }

        MessageResponse replySummary = null;
        if (message.getReplyToMessage() != null) {
            Message parent = message.getReplyToMessage();
            User parentSender = parent.getSender();
            Employee parentEmp = parentSender != null ? parentSender.getEmployee() : null;

            MessageSenderResponse parentSenderDto = new MessageSenderResponse(
                    parentSender != null ? parentSender.getId() : null,
                    parentEmp != null ? parentEmp.getEmployeeCode() : null,
                    parentEmp != null ? parentEmp.getFullName() : null,
                    parentEmp != null ? parentEmp.getFirstName() : null,
                    parentEmp != null ? parentEmp.getLastName() : null,
                    parentEmp != null ? parentEmp.getProfilePhotoUrl() : null,
                    parentEmp != null ? parentEmp.getDesignation() : null
            );

            replySummary = new MessageResponse(
                    parent.getId(),
                    parent.getConversation() != null ? parent.getConversation().getId() : null,
                    parent.getChannel() != null ? parent.getChannel().getId() : null,
                    parentSenderDto,
                    parent.getMessageType(),
                    parent.isDeleted() ? null : parent.getContent(),
                    null,
                    null,
                    Collections.emptyList(),
                    parent.isPinned(),
                    parent.isDeleted(),
                    parent.getCreatedAt(),
                    parent.getEditedAt(),
                    parent.getDeletedAt()
            );
        }

        String effectiveContent = message.isDeleted() ? null : message.getContent();

        return new MessageResponse(
                message.getId(),
                message.getConversation() != null ? message.getConversation().getId() : null,
                message.getChannel() != null ? message.getChannel().getId() : null,
                sender,
                message.getMessageType(),
                effectiveContent,
                message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null,
                replySummary,
                attachmentResponses,
                message.isPinned(),
                message.isDeleted(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt()
        );
    }
}
