package com.finpay.wallet.shared.exception;

import java.math.BigDecimal;

/**
 * Thrown when a wallet operation would breach the user's configured
 * daily or monthly transaction limit.
 */
public class TransactionLimitExceededException extends RuntimeException {

    private final LimitType limitType;
    private final BigDecimal limit;
    private final BigDecimal spent;
    private final BigDecimal attempted;

    public enum LimitType { DAILY, MONTHLY }

    public TransactionLimitExceededException(LimitType limitType,
                                             BigDecimal limit,
                                             BigDecimal spent,
                                             BigDecimal attempted) {
        super(buildMessage(limitType, limit, spent, attempted));
        this.limitType = limitType;
        this.limit = limit;
        this.spent = spent;
        this.attempted = attempted;
    }

    private static String buildMessage(LimitType type, BigDecimal limit,
                                       BigDecimal spent, BigDecimal attempted) {
        BigDecimal remaining = limit.subtract(spent).max(BigDecimal.ZERO);
        return String.format(
                "%s transaction limit exceeded. Limit: $%s, Already spent: $%s, " +
                "Remaining: $%s, Attempted: $%s",
                type.name().charAt(0) + type.name().substring(1).toLowerCase(),
                limit.toPlainString(), spent.toPlainString(),
                remaining.toPlainString(), attempted.toPlainString());
    }

    public LimitType getLimitType() { return limitType; }
    public BigDecimal getLimit()    { return limit; }
    public BigDecimal getSpent()    { return spent; }
    public BigDecimal getAttempted(){ return attempted; }
}
