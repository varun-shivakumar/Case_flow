package com.caseflow.notifications.controller;

import com.caseflow.notifications.dto.NotificationRequest;
import com.caseflow.notifications.dto.NotificationResponse;
import com.caseflow.notifications.enums.NotificationCategory;
import com.caseflow.notifications.enums.NotificationStatus;
import com.caseflow.notifications.security.RoleGuard;
import com.caseflow.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(
    name = "Notifications",
    description = "Notification hub. Categories: CASE, HEARING, APPEAL, COMPLIANCE. " +
        "Use /my/* endpoints to read your own notifications. " +
        "Use /user/{userId}/* for admin-level lookups."
)
public class NotificationController {

    private final NotificationService notificationService;
    private final RoleGuard roleGuard;

    // ─── Admin: create ────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Create and send a notification (ADMIN / CLERK)",
        description = "Manually creates a notification for any user. Roles: ADMIN, CLERK."
    )
    public ResponseEntity<NotificationResponse> createNotification(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @Valid @RequestBody NotificationRequest request) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.createNotification(request));
    }

    @PostMapping("/internal")
    @Operation(
        summary = "Internal endpoint for service-to-service notification delivery",
        description = "Called by other microservices (case-service, appeal-service, compliance-service). " +
            "Payload: { userId, message, category, caseId? }"
    )
    public ResponseEntity<NotificationResponse> createInternalNotification(
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.createFromInternal(payload));
    }

    // ─── My notifications (auth user reads their own) ─────────────────────────

    @GetMapping("/my")
    @Operation(summary = "Get all my notifications (newest first)")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE", "LAWYER", "LITIGANT");
        roleGuard.requireUserId(userId);
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    @GetMapping("/my/unread")
    @Operation(summary = "Get my unread notifications")
    public ResponseEntity<List<NotificationResponse>> getMyUnread(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE", "LAWYER", "LITIGANT");
        roleGuard.requireUserId(userId);
        return ResponseEntity.ok(notificationService.getByUserAndStatus(userId, NotificationStatus.UNREAD));
    }

    @GetMapping("/my/count")
    @Operation(summary = "Get my unread notification count")
    public ResponseEntity<Map<String, Long>> getMyUnreadCount(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE", "LAWYER", "LITIGANT");
        roleGuard.requireUserId(userId);
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.countUnreadForUser(userId)));
    }

    @GetMapping("/my/category/{category}")
    @Operation(summary = "Get my notifications filtered by category (CASE, HEARING, APPEAL, COMPLIANCE)")
    public ResponseEntity<List<NotificationResponse>> getMyByCategory(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable NotificationCategory category) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE", "LAWYER", "LITIGANT");
        roleGuard.requireUserId(userId);
        return ResponseEntity.ok(notificationService.getByUserAndCategory(userId, category));
    }

    @PatchMapping("/my/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public ResponseEntity<Map<String, String>> markMyAllRead(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE", "LAWYER", "LITIGANT");
        roleGuard.requireUserId(userId);
        notificationService.markAllAsReadForUser(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // ─── Admin lookups by userId ──────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all notifications for a user (ADMIN / CLERK)")
    public ResponseEntity<List<NotificationResponse>> getByUser(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String userId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Get unread notifications for a user (ADMIN / CLERK)")
    public ResponseEntity<List<NotificationResponse>> getUnreadByUser(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String userId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(notificationService.getByUserAndStatus(userId, NotificationStatus.UNREAD));
    }

    @GetMapping("/user/{userId}/count")
    @Operation(summary = "Get unread notification count for a user (ADMIN / CLERK)")
    public ResponseEntity<Map<String, Long>> getUnreadCountByUser(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String userId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.countUnreadForUser(userId)));
    }

    @GetMapping("/user/{userId}/category/{category}")
    @Operation(summary = "Get notifications for a user filtered by category (ADMIN / CLERK)")
    public ResponseEntity<List<NotificationResponse>> getByUserAndCategory(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String userId,
            @PathVariable NotificationCategory category) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(notificationService.getByUserAndCategory(userId, category));
    }

    @PatchMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all notifications as read for a user (ADMIN / CLERK)")
    public ResponseEntity<Map<String, String>> markAllReadByUser(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String userId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        notificationService.markAllAsReadForUser(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read for userId: " + userId));
    }

    // ─── By notification ID ───────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification by ID")
    public ResponseEntity<NotificationResponse> getById(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE", "LAWYER", "LITIGANT");
        NotificationResponse notification = notificationService.getById(id);
        // FIX: non-admin roles may only read their own notifications
        enforceOwnership(userRole, userId, notification.getUserId());
        return ResponseEntity.ok(notification);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE", "LAWYER", "LITIGANT");
        NotificationResponse notification = notificationService.getById(id);
        // FIX: non-admin roles may only mark their own notifications as read
        enforceOwnership(userRole, userId, notification.getUserId());
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    // ─── By case / by category ────────────────────────────────────────────────

    @GetMapping("/case/{caseId}")
    @Operation(summary = "Get all notifications for a case (ADMIN / CLERK)")
    public ResponseEntity<List<NotificationResponse>> getByCase(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long caseId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(notificationService.getByCase(caseId));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get all notifications by category (ADMIN / CLERK)")
    public ResponseEntity<List<NotificationResponse>> getByCategory(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable NotificationCategory category) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(notificationService.getByCategory(category));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Enforces that non-admin roles can only access their own notifications.
     * ADMIN and CLERK may access any notification without restriction.
     */
    private void enforceOwnership(String userRole, String requestingUserId, String notificationOwnerId) {
        boolean isPrivileged = "ADMIN".equalsIgnoreCase(userRole) || "CLERK".equalsIgnoreCase(userRole);
        if (!isPrivileged && !notificationOwnerId.equals(requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only access your own notifications. Use GET /api/notifications/my instead.");
        }
    }
}
