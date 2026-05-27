package com.caseflow.appeals.controller;

import com.caseflow.appeals.dto.request.AppealRequest;
import com.caseflow.appeals.dto.response.AppealAuditResponse;
import com.caseflow.appeals.dto.response.AppealResponse;
import com.caseflow.appeals.entity.Appeal.AppealStatus;
import com.caseflow.appeals.service.AppealAuditService;
import com.caseflow.appeals.service.AppealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles all appeal-level HTTP endpoints.
 * Review endpoints live in {@link ReviewController}.
 *
 * Workflow: SUBMITTED ──► REVIEWED ──► DECIDED
 *                  └──────────────► CANCELLED
 */
@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
@Tag(name = "Appeals", description = "File, query, and cancel appeals")
public class AppealController {

    private final AppealService        appealService;
    private final AppealAuditService   auditService;
    private final RoleGuard            roleGuard;

    // ─── File an Appeal ───────────────────────────────────────────────────────

    @Operation(
        summary     = "File a new appeal",
        description = "File an appeal on a CLOSED case.\n" +
                      "- LITIGANT: only their own case.\n" +
                      "- LAWYER  : only cases where they are the assigned lawyer.\n" +
                      "- ADMIN   : any case.\n" +
                      "filedByUserId is auto-resolved from the JWT — do not send it in the body."
    )
    @PostMapping
    public ResponseEntity<AppealResponse> fileAppeal(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @Valid @RequestBody AppealRequest request) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(appealService.fileAppeal(request, currentUserId, userRole));
    }

    // ─── Query Appeals ────────────────────────────────────────────────────────

    @Operation(summary = "Get appeal by ID")
    @GetMapping("/{id}")
    public ResponseEntity<AppealResponse> getAppeal(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "JUDGE", "CLERK", "ADMIN");
        return ResponseEntity.ok(appealService.getAppealById(id));
    }

    @Operation(summary = "Get all appeals for a case")
    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<AppealResponse>> getAppealsByCase(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long caseId) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "JUDGE", "CLERK", "ADMIN");
        return ResponseEntity.ok(appealService.getAppealsByCase(caseId));
    }

    @Operation(
        summary     = "Get my appeals",
        description = "Returns all appeals filed by the currently logged-in user (auto-resolved from JWT)."
    )
    @GetMapping("/my")
    public ResponseEntity<List<AppealResponse>> getMyAppeals(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "ADMIN");
        roleGuard.requireUserId(currentUserId);
        return ResponseEntity.ok(appealService.getAppealsByUser(currentUserId));
    }

    @Operation(
        summary     = "Get appeals filed by a specific user",
        description = "ADMIN can query any userId. LITIGANT/LAWYER may only query their own — use /my instead."
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AppealResponse>> getAppealsByUser(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String userId) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "ADMIN");
        if (!"ADMIN".equalsIgnoreCase(userRole) && !userId.equals(currentUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You can only view your own appeals. Use GET /api/appeals/my instead.");
        }
        return ResponseEntity.ok(appealService.getAppealsByUser(userId));
    }

    @Operation(
        summary     = "Get appeals filtered by status",
        description = "Valid statuses: SUBMITTED, REVIEWED, DECIDED, CANCELLED. Restricted to ADMIN, CLERK, JUDGE."
    )
    @GetMapping("/status/{status}")
    public ResponseEntity<List<AppealResponse>> getAppealsByStatus(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable AppealStatus status) {

        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE");
        return ResponseEntity.ok(appealService.getAppealsByStatus(status));
    }

    // ─── Audit Trail ─────────────────────────────────────────────────────────

    @Operation(
        summary     = "Get the full audit trail for an appeal",
        description = "Append-only record of every state-changing action: filings, cancellations, " +
                      "assignments, draft outcome changes, decisions, document uploads/deletions. " +
                      "Newest first. Restricted to the filer, the assigned judge, CLERK, and ADMIN."
    )
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AppealAuditResponse>> getAuditTrail(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "JUDGE", "CLERK", "ADMIN");
        roleGuard.requireUserId(currentUserId);

        // LITIGANT/LAWYER may only view audit trails for appeals they filed.
        boolean isPrivileged = "ADMIN".equalsIgnoreCase(userRole)
                            || "CLERK".equalsIgnoreCase(userRole)
                            || "JUDGE".equalsIgnoreCase(userRole);
        if (!isPrivileged) {
            AppealResponse appeal = appealService.getAppealById(id);
            if (!currentUserId.equals(appeal.getFiledByUserId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only view the audit trail for appeals you filed.");
            }
        }
        return ResponseEntity.ok(auditService.getAuditTrail(id));
    }

    // ─── Cancel an Appeal ─────────────────────────────────────────────────────

    @Operation(
        summary     = "Cancel / withdraw an appeal",
        description = "- LITIGANT/LAWYER: can cancel only their own SUBMITTED appeal.\n" +
                      "- ADMIN          : can cancel any SUBMITTED or REVIEWED appeal.\n" +
                      "- DECIDED and already-CANCELLED appeals cannot be cancelled."
    )
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppealResponse> cancelAppeal(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "ADMIN");
        roleGuard.requireUserId(currentUserId);
        return ResponseEntity.ok(appealService.cancelAppeal(id, currentUserId, userRole));
    }
}
