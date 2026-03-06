package com.finpay.user.audit;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        AuditLog.AuditAction action,
        AuditLog.TargetType targetType,
        String targetId,
        String description,
        String previousState,
        String newState,
        String ipAddress,
        String serviceSource,
        LocalDateTime createdAt
) {
    public static AuditLogResponse fromEntity(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorId(),
                auditLog.getActorEmail(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getDescription(),
                auditLog.getPreviousState(),
                auditLog.getNewState(),
                auditLog.getIpAddress(),
                auditLog.getServiceSource(),
                auditLog.getCreatedAt()
        );
    }
}
