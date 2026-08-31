package com.logiconnect.platform.notification.service;

import com.logiconnect.platform.common.exception.BadRequestException;
import com.logiconnect.platform.common.exception.ResourceNotFoundException;
import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.notification.dto.NotificationResponse;
import com.logiconnect.platform.notification.dto.UnreadCountResponse;
import com.logiconnect.platform.notification.entity.Notification;
import com.logiconnect.platform.notification.entity.NotificationType;
import com.logiconnect.platform.notification.repository.NotificationRepository;
import com.logiconnect.platform.user.entity.User;
import com.logiconnect.platform.user.entity.UserStatus;
import com.logiconnect.platform.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification createNotification(UUID recipientId, NotificationType type, String title, String message, String referenceType, UUID referenceId) {
        if (recipientId == null) {
            throw new BadRequestException("Recipient user ID must not be null.");
        }
        if (type == null) {
            throw new BadRequestException("Notification type must not be null.");
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Notification title must not be blank.");
        }
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Notification message must not be blank.");
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", recipientId));

        if (recipient.getStatus() != UserStatus.ACTIVE) {
            log.debug("Skipping notification creation for inactive user: {}", recipientId);
            return null;
        }

        // Deduplication check if reference is provided
        if (referenceType != null && referenceId != null) {
            boolean exists = notificationRepository.existsByUserIdAndReferenceTypeAndReferenceIdAndType(
                    recipientId, referenceType, referenceId, type
            );
            if (exists) {
                log.debug("Notification already exists for user {} and ref {}:{}:{}", recipientId, referenceType, referenceId, type);
                return null;
            }
        }

        Notification notification = new Notification(recipient, type, title.trim(), message.trim(), referenceType, referenceId);
        Notification saved = notificationRepository.save(notification);
        log.debug("Notification created: id={}, user={}, type={}", saved.getId(), recipientId, type);
        return saved;
    }

    @Transactional
    public List<Notification> createBulkNotifications(Collection<UUID> recipientIds, NotificationType type, String title, String message, String referenceType, UUID referenceId) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (type == null) {
            throw new BadRequestException("Notification type must not be null.");
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Notification title must not be blank.");
        }
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Notification message must not be blank.");
        }

        Set<UUID> uniqueRecipientIds = new HashSet<>(recipientIds);
        List<User> recipients = userRepository.findAllById(uniqueRecipientIds);

        List<Notification> notificationsToSave = new ArrayList<>();
        for (User recipient : recipients) {
            if (recipient.getStatus() != UserStatus.ACTIVE) {
                continue;
            }
            if (referenceType != null && referenceId != null) {
                boolean exists = notificationRepository.existsByUserIdAndReferenceTypeAndReferenceIdAndType(
                        recipient.getId(), referenceType, referenceId, type
                );
                if (exists) {
                    continue;
                }
            }
            notificationsToSave.add(new Notification(recipient, type, title.trim(), message.trim(), referenceType, referenceId));
        }

        if (notificationsToSave.isEmpty()) {
            return Collections.emptyList();
        }

        List<Notification> saved = notificationRepository.saveAll(notificationsToSave);
        log.info("Bulk notifications created: count={}, type={}, ref={}:{}", saved.size(), type, referenceType, referenceId);
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(UUID userId, Boolean unreadOnly, Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.min(100, Math.max(1, pageable.getPageSize()));
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<Notification> notificationPage;
        if (Boolean.TRUE.equals(unreadOnly)) {
            notificationPage = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDescIdDesc(userId, false, pageRequest);
        } else {
            notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageRequest);
        }

        return PageResponse.from(notificationPage.map(NotificationResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.isRead()) {
            notification.markAsRead(Instant.now());
            notification = notificationRepository.save(notification);
            log.debug("Notification marked as read: id={}, user={}", notificationId, userId);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        int updatedCount = notificationRepository.markAllAsReadForUser(userId, Instant.now());
        log.debug("All notifications marked as read for user: {}, count={}", userId, updatedCount);
        return updatedCount;
    }
}
