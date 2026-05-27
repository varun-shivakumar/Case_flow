package com.caseflow.iam.controller;

import com.caseflow.iam.entity.AuditLog;
import com.caseflow.iam.entity.User;
import com.caseflow.iam.service.CachedUserService;
import com.caseflow.iam.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/paginated")
@RequiredArgsConstructor
@Tag(name = "User Management — Paginated", description = "Paginated and cached user endpoints")
public class PaginatedUserController {

    private final CachedUserService cachedUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (paginated)",
        description = "Params: page (0-based), size (default 10), sort (e.g. name,asc)")
    public ResponseEntity<Page<User>> getAllUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId,asc") String sort) {
        String[] s = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(s.length > 1 && s[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, s[0]));
        return ResponseEntity.ok(cachedUserService.getAllUsersPaginated(pageable));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all audit logs (paginated)")
    public ResponseEntity<Page<AuditLog>> getAllAuditLogsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(cachedUserService.getAllAuditLogsPaginated(PageRequest.of(page, size)));
    }

    @GetMapping("/cached/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLERK')")
    @Operation(summary = "Get user by ID (cached)")
    public ResponseEntity<UserResponse> getCachedUser(@PathVariable String id) {
        return ResponseEntity.ok(cachedUserService.getCachedUserById(id));
    }
}
