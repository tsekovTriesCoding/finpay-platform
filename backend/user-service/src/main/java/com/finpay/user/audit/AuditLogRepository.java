package com.finpay.user.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    Page<AuditLog> findByTargetTypeOrderByCreatedAtDesc(AuditLog.TargetType targetType, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditLog.AuditAction action, Pageable pageable);

    Page<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            AuditLog.TargetType targetType, String targetId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actorId IS NULL OR a.actorId = :actorId) AND " +
           "(:targetType IS NULL OR a.targetType = :targetType) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("actorId") UUID actorId,
            @Param("targetType") AuditLog.TargetType targetType,
            @Param("action") AuditLog.AuditAction action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    List<AuditLog> findByTargetIdOrderByCreatedAtDesc(String targetId);

    long countByCreatedAtAfter(LocalDateTime since);

    long countByActionAndCreatedAtAfter(AuditLog.AuditAction action, LocalDateTime since);
}
