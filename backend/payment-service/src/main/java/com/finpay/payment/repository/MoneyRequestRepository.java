package com.finpay.payment.repository;

import com.finpay.payment.entity.MoneyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, UUID> {

    Optional<MoneyRequest> findByRequestReference(String requestReference);

    /** All requests where the user is either the requester or the payer. */
    @Query("SELECT r FROM MoneyRequest r WHERE r.requesterUserId = :userId OR r.payerUserId = :userId ORDER BY r.createdAt DESC")
    Page<MoneyRequest> findByUserIdAsParticipant(@Param("userId") UUID userId, Pageable pageable);

    /** Pending requests where this user is the payer (incoming requests to approve/decline). */
    @Query("SELECT r FROM MoneyRequest r WHERE r.payerUserId = :userId AND r.status = 'PENDING_APPROVAL' ORDER BY r.createdAt DESC")
    Page<MoneyRequest> findPendingForPayer(@Param("userId") UUID userId, Pageable pageable);

    /** Pending requests created by this user (outgoing requests waiting for approval). */
    @Query("SELECT r FROM MoneyRequest r WHERE r.requesterUserId = :userId AND r.status = 'PENDING_APPROVAL' ORDER BY r.createdAt DESC")
    Page<MoneyRequest> findPendingByRequester(@Param("userId") UUID userId, Pageable pageable);

    /** Count of pending incoming requests for a payer (badge count). */
    @Query("SELECT COUNT(r) FROM MoneyRequest r WHERE r.payerUserId = :userId AND r.status = 'PENDING_APPROVAL'")
    long countPendingForPayer(@Param("userId") UUID userId);

    /** Find expired requests that should be marked as EXPIRED. */
    @Query("SELECT r FROM MoneyRequest r WHERE r.status = 'PENDING_APPROVAL' AND r.expiresAt < :now")
    List<MoneyRequest> findExpiredRequests(@Param("now") LocalDateTime now);

    /** Find requests needing compensation. */
    @Query("SELECT r FROM MoneyRequest r WHERE r.sagaStatus = :sagaStatus AND r.compensationRequired = true AND r.compensationCompleted = false")
    List<MoneyRequest> findPendingCompensations(@Param("sagaStatus") MoneyRequest.SagaStatus sagaStatus);
}
