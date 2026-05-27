package com.caseflow.iam.controller;

import com.caseflow.iam.dto.*;
import com.caseflow.iam.entity.AuditLog;
import com.caseflow.iam.entity.User;
import com.caseflow.iam.service.AuditLogService;
import com.caseflow.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "2. User Management (4.1)",
        description = "Module 4.1 — Admin-only user management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user account (Admin only)")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.createUserByAdmin(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all registered users (Admin only)")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN','CLERK','LITIGANT','LAWYER','JUDGE')")
    @Operation(summary = "Get all active users by role (e.g. LAWYER, LITIGANT)")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLERK')")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate a user account (Admin only)")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable String id,
            @RequestParam User.Status status) {
        return ResponseEntity.ok(userService.updateStatus(id, status));
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force reset any user's password (Admin only)")
    public ResponseEntity<String> adminResetPassword(
            @PathVariable String id,
            @RequestParam String newPassword) {
        return ResponseEntity.ok(userService.adminResetPassword(id, newPassword));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all system audit logs (Admin only)")
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @GetMapping("/audit-logs/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs for a specific user (Admin only)")
    public ResponseEntity<List<AuditLog>> getLogsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(auditLogService.getLogsByUser(userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a user account permanently (Admin only)")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.deleteUser(id));
    }

    @GetMapping("/exists/{id}")
    @Operation(summary = "Check if a user exists by ID (internal use by other services)")
    public ResponseEntity<Boolean> existsById(@PathVariable String id) {
        return ResponseEntity.ok(userService.existsById(id));
    }

    @GetMapping("/{id}/role")
    @Operation(summary = "Get the role of a user by ID (internal use by other services)")
    public ResponseEntity<String> getUserRole(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserRole(id));
    }
}
