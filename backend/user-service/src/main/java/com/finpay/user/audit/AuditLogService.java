package com.finpay.user.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Record an audit log entry for an admin action.
     */
    public AuditLog logAction(UUID actorId, String actorEmail, AuditLog.AuditAction action,
                              AuditLog.TargetType targetType, String targetId,
                              String description, String previousState, String newState,
                              String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .description(description)
                .previousState(previousState)
                .newState(newState)
                .ipAddress(ipAddress)
                .serviceSource("user-service")
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log created: {} by {} on {} {}", action, actorEmail, targetType, targetId);
        return saved;
    }

    /**
     * Simplified log action without state tracking.
     */
    public AuditLog logAction(UUID actorId, String actorEmail, AuditLog.AuditAction action,
                              AuditLog.TargetType targetType, String targetId,
                              String description, String ipAddress) {
        return logAction(actorId, actorEmail, action, targetType, targetId,
                description, null, null, ipAddress);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchAuditLogs(UUID actorId, AuditLog.TargetType targetType,
                                                   AuditLog.AuditAction action,
                                                   LocalDateTime startDate, LocalDateTime endDate,
                                                   int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return auditLogRepository.searchAuditLogs(actorId, targetType, action, startDate, endDate, pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByTarget(AuditLog.TargetType targetType, String targetId,
                                                        int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId, pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long countRecentActions(LocalDateTime since) {
        return auditLogRepository.countByCreatedAtAfter(since);
    }
}
