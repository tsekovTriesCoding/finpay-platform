package com.finpay.payment.repository;

import com.finpay.payment.entity.MoneyTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MoneyTransferRepository extends JpaRepository<MoneyTransfer, UUID> {

    Optional<MoneyTransfer> findByTransactionReference(String transactionReference);

    @Query("SELECT t FROM MoneyTransfer t WHERE t.senderUserId = :userId OR t.recipientUserId = :userId ORDER BY t.createdAt DESC")
    Page<MoneyTransfer> findByUserIdAsParticipant(@Param("userId") UUID userId, Pageable pageable);

    List<MoneyTransfer> findBySenderUserId(UUID senderUserId);

    List<MoneyTransfer> findByRecipientUserId(UUID recipientUserId);

    @Query("SELECT t FROM MoneyTransfer t WHERE t.status = :status")
    List<MoneyTransfer> findByStatus(@Param("status") MoneyTransfer.TransferStatus status);

    @Query("SELECT t FROM MoneyTransfer t WHERE t.sagaStatus = :sagaStatus AND t.compensationRequired = true AND t.compensationCompleted = false")
    List<MoneyTransfer> findPendingCompensations(@Param("sagaStatus") MoneyTransfer.SagaStatus sagaStatus);
}
