package com.finpay.payment.mapper;

import com.finpay.payment.dto.PaymentRequest;
import com.finpay.payment.dto.PaymentResponse;
import com.finpay.payment.entity.Payment;
import com.finpay.payment.event.PaymentEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionReference", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "sourceAccountNumber", ignore = true)
    @Mapping(target = "sourceAccountName", ignore = true)
    @Mapping(target = "sourceBankCode", ignore = true)
    @Mapping(target = "cardLastFourDigits", ignore = true)
    @Mapping(target = "cardType", ignore = true)
    @Mapping(target = "gatewayReference", ignore = true)
    @Mapping(target = "gatewayResponse", ignore = true)
    @Mapping(target = "processingFee", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Payment toEntity(PaymentRequest request);

    PaymentResponse toResponse(Payment payment);

    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now())")
    PaymentEvent toEvent(Payment payment, PaymentEvent.EventType eventType);
}
