package com.finpay.user.admin;

public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long suspendedUsers,
        long pendingVerification,
        long adminCount,
        long merchantCount,
        long regularUserCount,
        long recentAuditActions24h,
        long recentAuditActions7d
) {
}
