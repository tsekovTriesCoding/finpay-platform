package com.finpay.payment.billpayment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, UUID> {

    Optional<BillPayment> findByTransactionReference(String transactionReference);

    List<BillPayment> findByUserId(UUID userId);

    Page<BillPayment> findByUserId(UUID userId, Pageable pageable);

    Page<BillPayment> findByUserIdAndCategory(UUID userId, BillPayment.BillCategory category, Pageable pageable);

    List<BillPayment> findByUserIdAndStatus(UUID userId, BillPayment.BillPaymentStatus status);

    Page<BillPayment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
