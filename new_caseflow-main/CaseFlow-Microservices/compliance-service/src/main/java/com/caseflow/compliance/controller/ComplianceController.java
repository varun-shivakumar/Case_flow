package com.caseflow.compliance.controller;

import com.caseflow.compliance.dto.AuditRequest;
import com.caseflow.compliance.dto.AuditResponse;
import com.caseflow.compliance.dto.ComplianceCheckRequest;
import com.caseflow.compliance.dto.ComplianceRecordResponse;
import com.caseflow.compliance.dto.ComplianceRunSummary;
import com.caseflow.compliance.security.RoleGuard;
import com.caseflow.compliance.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(
    name = "Compliance & Audit",
    description = "Compliance checks and audit management. " +
        "Types: DOCUMENT (all docs verified), PROCESS (no SLA breaches). " +
        "Workflow: Step 1 — Run Check → Step 2 — Create Audit (OPEN) → Step 3 — Update Findings → Step 4 — Close Audit"
)
public class ComplianceController {

    private final ComplianceService complianceService;
    private final RoleGuard roleGuard;

    @PostMapping("/api/compliance/check")
    @Operation(
        summary = "Run compliance check on cases (Step 1)",
        description = "Performs automated DOCUMENT and PROCESS compliance checks. " +
            "If no caseIds are provided, all cases are checked. Roles: ADMIN, CLERK."
    )
    public ResponseEntity<List<ComplianceRecordResponse>> runComplianceCheck(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @Valid @RequestBody ComplianceCheckRequest request) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(complianceService.runComplianceCheck(request, userId));
    }

    @GetMapping("/api/compliance/case/{caseId}")
    @Operation(summary = "Get all compliance records for a case")
    public ResponseEntity<List<ComplianceRecordResponse>> getByCase(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long caseId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(complianceService.getComplianceRecordsByCase(caseId));
    }

    @PostMapping("/api/audits")
    @Operation(
        summary = "Create a new audit record (Step 2)",
        description = "Creates an audit with status OPEN. The creating admin is resolved from the auth token. Roles: ADMIN, CLERK."
    )
    public ResponseEntity<AuditResponse> createAudit(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @Valid @RequestBody AuditRequest request) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complianceService.createAudit(request, userId));
    }

    @GetMapping("/api/audits/{id}")
    @Operation(summary = "Get audit record by ID")
    public ResponseEntity<AuditResponse> getAudit(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(complianceService.getAuditById(id));
    }

    @PatchMapping("/api/audits/{id}/findings")
    @Operation(
        summary = "Update findings for an open audit (Step 3)",
        description = "Only allowed on OPEN audits. Roles: ADMIN, CLERK."
    )
    public ResponseEntity<AuditResponse> updateFindings(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id,
            @RequestBody String findings) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(complianceService.updateFindings(id, findings));
    }

    @PatchMapping("/api/audits/{id}/close")
    @Operation(
        summary = "Close an audit (Step 4)",
        description = "Transitions an OPEN audit to CLOSED. Admin identity is resolved from the auth token. Roles: ADMIN, CLERK."
    )
    public ResponseEntity<AuditResponse> closeAudit(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(complianceService.closeAudit(id, userId));
    }

    @GetMapping("/api/audits/admin/{adminId}")
    @Operation(summary = "Get all audits created by an admin")
    public ResponseEntity<List<AuditResponse>> getAuditsByAdmin(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String adminId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(complianceService.getAuditsByAdmin(adminId));
    }

    @DeleteMapping("/api/compliance/{complianceId}")
    @Operation(summary = "Delete a compliance record", description = "Permanently removes a compliance record. Roles: ADMIN.")
    public ResponseEntity<Void> deleteComplianceRecord(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long complianceId) {
        roleGuard.requireAnyRole(userRole, "ADMIN");
        complianceService.deleteComplianceRecord(complianceId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/audits/{id}")
    @Operation(summary = "Delete an audit record", description = "Permanently removes an audit. Roles: ADMIN.")
    public ResponseEntity<Void> deleteAudit(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {
        roleGuard.requireAnyRole(userRole, "ADMIN");
        complianceService.deleteAudit(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/compliance/runs")
    @Operation(summary = "List compliance check runs",
        description = "One entry per Run-Compliance-Check invocation. Records that pre-date run tracking are aggregated by date.")
    public ResponseEntity<List<ComplianceRunSummary>> listRuns(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(complianceService.getAllRuns());
    }

    /* ── Bulk delete: by id list ─────────────────────────────────────────── */

    @PostMapping("/api/compliance/bulk-delete")
    @Operation(summary = "Bulk-delete compliance records by id",
        description = "Body: { ids: [1,2,3] }. Returns { deleted: N }. Roles: ADMIN.")
    public ResponseEntity<java.util.Map<String, Object>> bulkDeleteComplianceRecords(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @RequestBody java.util.Map<String, java.util.List<Long>> body) {
        roleGuard.requireAnyRole(userRole, "ADMIN");
        long deleted = complianceService.deleteComplianceRecords(
                body == null ? java.util.List.of() : body.getOrDefault("ids", java.util.List.of()));
        return ResponseEntity.ok(java.util.Map.of("deleted", deleted));
    }

    @PostMapping("/api/audits/bulk-delete")
    @Operation(summary = "Bulk-delete audits by id",
        description = "Body: { ids: [1,2,3] }. Returns { deleted: N }. Roles: ADMIN.")
    public ResponseEntity<java.util.Map<String, Object>> bulkDeleteAudits(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @RequestBody java.util.Map<String, java.util.List<Long>> body) {
        roleGuard.requireAnyRole(userRole, "ADMIN");
        long deleted = complianceService.deleteAudits(
                body == null ? java.util.List.of() : body.getOrDefault("ids", java.util.List.of()));
        return ResponseEntity.ok(java.util.Map.of("deleted", deleted));
    }

    @GetMapping("/api/compliance/runs/{runId}")
    @Operation(summary = "Get the per-case records of a single compliance run")
    public ResponseEntity<List<ComplianceRecordResponse>> getRunRecords(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String runId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(complianceService.getRunRecords(runId));
    }
}
