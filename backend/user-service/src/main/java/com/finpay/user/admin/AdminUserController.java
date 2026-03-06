package com.finpay.user.admin;

import com.finpay.user.audit.AuditLog;
import com.finpay.user.audit.AuditLogResponse;
import com.finpay.user.audit.AuditLogService;
import com.finpay.user.dto.UserResponse;
import com.finpay.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoints for user management.
 * Security is enforced at the API Gateway level (AdminAuthFilter checks ADMIN role).
 * X-User-Id and X-User-Role headers are injected by the gateway after JWT validation.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;

    /**
     * List all users with server-side pagination, sorting, and filtering.
     * Supports: search (name/email), status filter, role filter, sort field/direction.
     */
    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) User.UserStatus status,
            @RequestParam(required = false) User.UserRole role,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<UserResponse> users = adminUserService.listUsers(search, status, role, sortBy, sortDir, page, size);
        return ResponseEntity.ok(users);
    }

    /**
     * Get a single user's details.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        List<UserResponse> users = adminUserService.getUsersByRole(null);
        // Use the existing service to get by ID
        return ResponseEntity.ok(adminUserService.listUsers(null, null, null, "createdAt", "desc", 0, 1)
                .getContent().stream().filter(u -> u.id().equals(userId)).findFirst()
                .orElseThrow(() -> new com.finpay.user.exception.ResourceNotFoundException("User not found")));
    }

    /**
     * Change a user's role.
     * PATCH /api/v1/admin/users/{userId}/role
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String adminIdStr,
            @RequestHeader("X-User-Email") String adminEmail,
            HttpServletRequest request) {

        UUID adminId = UUID.fromString(adminIdStr);
        User.UserRole newRole = User.UserRole.valueOf(body.get("role"));
        String ip = request.getRemoteAddr();

        UserResponse response = adminUserService.changeUserRole(userId, newRole, adminId, adminEmail, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * Suspend a user account.
     * POST /api/v1/admin/users/{userId}/suspend
     */
    @PostMapping("/{userId}/suspend")
    public ResponseEntity<UserResponse> suspendUser(
            @PathVariable UUID userId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader("X-User-Id") String adminIdStr,
            @RequestHeader("X-User-Email") String adminEmail,
            HttpServletRequest request) {

        UUID adminId = UUID.fromString(adminIdStr);
        String reason = body != null ? body.get("reason") : null;
        String ip = request.getRemoteAddr();

        UserResponse response = adminUserService.suspendUser(userId, reason, adminId, adminEmail, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * Unsuspend (reactivate) a user account.
     * POST /api/v1/admin/users/{userId}/unsuspend
     */
    @PostMapping("/{userId}/unsuspend")
    public ResponseEntity<UserResponse> unsuspendUser(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String adminIdStr,
            @RequestHeader("X-User-Email") String adminEmail,
            HttpServletRequest request) {

        UUID adminId = UUID.fromString(adminIdStr);
        String ip = request.getRemoteAddr();

        UserResponse response = adminUserService.unsuspendUser(userId, adminId, adminEmail, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * Force password reset for a user.
     * POST /api/v1/admin/users/{userId}/force-password-reset
     */
    @PostMapping("/{userId}/force-password-reset")
    public ResponseEntity<UserResponse> forcePasswordReset(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String adminIdStr,
            @RequestHeader("X-User-Email") String adminEmail,
            HttpServletRequest request) {

        UUID adminId = UUID.fromString(adminIdStr);
        String ip = request.getRemoteAddr();

        UserResponse response = adminUserService.forcePasswordReset(userId, adminId, adminEmail, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * Get users by role — uses the previously unused findByRole() repository method.
     */
    @GetMapping("/by-role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable User.UserRole role) {
        List<UserResponse> users = adminUserService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    /**
     * Get admin dashboard KPI metrics.
     */
    @GetMapping("/dashboard/metrics")
    public ResponseEntity<AdminDashboardResponse> getDashboardMetrics() {
        AdminDashboardResponse metrics = adminUserService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }

    // Audit log endpoints

    /**
     * Get audit logs with pagination.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.getAuditLogs(page, size));
    }

    /**
     * Search audit logs with filters.
     */
    @GetMapping("/audit-logs/search")
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) AuditLog.TargetType targetType,
            @RequestParam(required = false) AuditLog.AuditAction action,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.searchAuditLogs(
                actorId, targetType, action, startDate, endDate, page, size));
    }

    /**
     * Get audit logs for a specific target resource.
     */
    @GetMapping("/audit-logs/target/{targetType}/{targetId}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByTarget(
            @PathVariable AuditLog.TargetType targetType,
            @PathVariable String targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByTarget(targetType, targetId, page, size));
    }
}
