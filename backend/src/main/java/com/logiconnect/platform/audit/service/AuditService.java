package com.logiconnect.platform.audit.service;

import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.entity.AuditLog;
import com.logiconnect.platform.audit.repository.AuditLogRepository;
import com.logiconnect.platform.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Records an immutable security audit entry.
     */
    @Transactional
    public void recordAuthEvent(
            User actor,
            AuditAction action,
            String entityType,
            UUID entityId,
            String ipAddress,
            String userAgent,
            Map<String, Object> metadata
    ) {
        try {
            AuditLog auditLog = new AuditLog();
            if (actor != null) {
                auditLog.setActorId(actor.getId());
            }
            auditLog.setAction(action.name());
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setMetadata(metadata);

            auditLogRepository.save(auditLog);

            log.info("AUDIT_EVENT: action={}, actorId={}, entityType={}, entityId={}, ipAddress={}",
                    action, actor != null ? actor.getId() : "ANONYMOUS", entityType, entityId, ipAddress);
        } catch (Exception e) {
            log.error("Failed to persist audit log for action: {}", action, e);
        }
    }
}
