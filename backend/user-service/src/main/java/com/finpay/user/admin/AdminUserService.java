package com.finpay.user.admin;

import com.finpay.user.audit.AuditLog;
import com.finpay.user.audit.AuditLogService;
import com.finpay.user.dto.UserResponse;
import com.finpay.user.entity.User;
import com.finpay.user.event.UserEvent;
import com.finpay.user.exception.ResourceNotFoundException;
import com.finpay.user.mapper.UserMapper;
import com.finpay.user.repository.UserRepository;
import com.finpay.user.service.UserEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEventProducer userEventProducer;
    private final AuditLogService auditLogService;

    /**
     * List all users with server-side pagination, sorting, and filtering.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String search, User.UserStatus status, User.UserRole role,
                                         String sortBy, String sortDir, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        if (role != null) {
            if (status != null) {
                return userRepository.findByRoleAndStatus(role, status, pageable)
                        .map(userMapper::toResponse);
            }
            return userRepository.findByRole(role, pageable)
                    .map(userMapper::toResponse);
        }

        if (status != null) {
            return userRepository.findByStatus(status, pageable)
                    .map(userMapper::toResponse);
        }

        if (search != null && !search.isBlank()) {
            return userRepository.adminSearchUsers(search.trim(), pageable)
                    .map(userMapper::toResponse);
        }

        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    /**
     * Change a user's role (assign/revoke).
     */
    public UserResponse changeUserRole(UUID userId, User.UserRole newRole,
                                        UUID adminId, String adminEmail, String ipAddress) {
        User user = findUserOrThrow(userId);
        User.UserRole oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        auditLogService.logAction(adminId, adminEmail,
                AuditLog.AuditAction.USER_ROLE_CHANGED,
                AuditLog.TargetType.USER, userId.toString(),
                String.format("Role changed from %s to %s for user %s", oldRole, newRole, user.getEmail()),
                oldRole.name(), newRole.name(), ipAddress);

        log.info("Admin {} changed role of user {} from {} to {}", adminEmail, userId, oldRole, newRole);
        return userMapper.toResponse(user);
    }

    /**
     * Suspend a user account.
     */
    public UserResponse suspendUser(UUID userId, String reason,
                                     UUID adminId, String adminEmail, String ipAddress) {
        User user = findUserOrThrow(userId);
        User.UserStatus oldStatus = user.getStatus();
        user.setStatus(User.UserStatus.SUSPENDED);
        userRepository.save(user);

        userEventProducer.sendUserEvent(userMapper.toEvent(user, UserEvent.EventType.USER_STATUS_CHANGED));

        auditLogService.logAction(adminId, adminEmail,
                AuditLog.AuditAction.USER_SUSPENDED,
                AuditLog.TargetType.USER, userId.toString(),
                String.format("User %s suspended. Reason: %s. Previous status: %s",
                        user.getEmail(), reason != null ? reason : "No reason provided", oldStatus),
                oldStatus.name(), User.UserStatus.SUSPENDED.name(), ipAddress);

        log.info("Admin {} suspended user {}", adminEmail, userId);
        return userMapper.toResponse(user);
    }

    /**
     * Unsuspend (reactivate) a user account.
     */
    public UserResponse unsuspendUser(UUID userId,
                                       UUID adminId, String adminEmail, String ipAddress) {
        User user = findUserOrThrow(userId);
        User.UserStatus oldStatus = user.getStatus();
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        userEventProducer.sendUserEvent(userMapper.toEvent(user, UserEvent.EventType.USER_STATUS_CHANGED));

        auditLogService.logAction(adminId, adminEmail,
                AuditLog.AuditAction.USER_UNSUSPENDED,
                AuditLog.TargetType.USER, userId.toString(),
                String.format("User %s unsuspended. Previous status: %s", user.getEmail(), oldStatus),
                oldStatus.name(), User.UserStatus.ACTIVE.name(), ipAddress);

        log.info("Admin {} unsuspended user {}", adminEmail, userId);
        return userMapper.toResponse(user);
    }

    /**
     * Force password reset for a user (marks account for password change).
     */
    public UserResponse forcePasswordReset(UUID userId,
                                            UUID adminId, String adminEmail, String ipAddress) {
        User user = findUserOrThrow(userId);
        // Mark user status to trigger password reset flow on next login
        user.setStatus(User.UserStatus.PENDING_VERIFICATION);
        userRepository.save(user);

        auditLogService.logAction(adminId, adminEmail,
                AuditLog.AuditAction.USER_FORCE_PASSWORD_RESET,
                AuditLog.TargetType.USER, userId.toString(),
                String.format("Forced password reset for user %s", user.getEmail()),
                ipAddress);

        log.info("Admin {} forced password reset for user {}", adminEmail, userId);
        return userMapper.toResponse(user);
    }

    /**
     * Get KPI dashboard metrics.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardMetrics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findByStatus(User.UserStatus.ACTIVE).size();
        long suspendedUsers = userRepository.findByStatus(User.UserStatus.SUSPENDED).size();
        long pendingVerification = userRepository.findByStatus(User.UserStatus.PENDING_VERIFICATION).size();

        // Users by role
        long adminCount = userRepository.findByRole(User.UserRole.ADMIN).size();
        long merchantCount = userRepository.findByRole(User.UserRole.MERCHANT).size();
        long regularUserCount = userRepository.findByRole(User.UserRole.USER).size();

        // Recent activity (last 24h, 7d, 30d)
        LocalDateTime now = LocalDateTime.now();
        long recentAuditActions24h = auditLogService.countRecentActions(now.minusHours(24));
        long recentAuditActions7d = auditLogService.countRecentActions(now.minusDays(7));

        return new AdminDashboardResponse(
                totalUsers, activeUsers, suspendedUsers, pendingVerification,
                adminCount, merchantCount, regularUserCount,
                recentAuditActions24h, recentAuditActions7d
        );
    }

    /**
     * Get users by role — uses the previously unused findByRole() method.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(User.UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }
}
