package com.finpay.wallet.transaction;

import com.finpay.wallet.transaction.dto.WalletTransactionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletTransactionMapper {

    WalletTransactionResponse toResponse(WalletTransaction transaction);
}
