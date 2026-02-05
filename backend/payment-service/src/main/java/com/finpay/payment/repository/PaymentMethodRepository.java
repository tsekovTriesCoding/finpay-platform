package com.finpay.payment.repository;

import com.finpay.payment.entity.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, UUID> {

    List<PaymentMethodEntity> findByUserId(UUID userId);

    List<PaymentMethodEntity> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<PaymentMethodEntity> findByUserIdAndIsDefaultTrue(UUID userId);

    List<PaymentMethodEntity> findByUserIdAndType(UUID userId, PaymentMethodEntity.MethodType type);

    boolean existsByUserIdAndCardToken(UUID userId, String cardToken);
}
