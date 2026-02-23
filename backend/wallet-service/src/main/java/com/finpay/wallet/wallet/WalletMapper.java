package com.finpay.wallet.wallet;

import com.finpay.wallet.wallet.dto.WalletResponse;
import com.finpay.wallet.wallet.event.WalletEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "availableBalance", expression = "java(wallet.getAvailableBalance())")
    @Mapping(target = "dailySpent", source = "spendTracker.dailySpent")
    @Mapping(target = "monthlySpent", source = "spendTracker.monthlySpent")
    @Mapping(target = "remainingDailyLimit", expression = "java(wallet.getRemainingDailyLimit())")
    @Mapping(target = "remainingMonthlyLimit", expression = "java(wallet.getRemainingMonthlyLimit())")
    WalletResponse toResponse(Wallet wallet);

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now())")
    WalletEvent toEvent(Wallet wallet, WalletEvent.EventType eventType,
                        java.math.BigDecimal amount, java.math.BigDecimal balanceBefore,
                        java.math.BigDecimal balanceAfter, String referenceId, String description);
}
