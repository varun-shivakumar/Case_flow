package com.caseflow.reporting.controller;

import com.caseflow.reporting.dto.ReportRequest;
import com.caseflow.reporting.dto.ReportResponse;
import com.caseflow.reporting.entity.Report.ReportScope;
import com.caseflow.reporting.security.RoleGuard;
import com.caseflow.reporting.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(
    name = "Reporting & Analytics",
    description = "Generate analytics reports aggregated from case, hearing, workflow, appeal, and compliance services. " +
        "Supported scopes: COURT (system-wide), JUDGE, PERIOD (date range), CLERK, LAWYER, CASE (single case), COMPLIANCE."
)
public class ReportController {

    private final ReportService reportService;
    private final RoleGuard roleGuard;

    @PostMapping
    @Operation(
        summary = "Generate a new analytics report",
        description = "Aggregates real data from downstream services. " +
            "requestedBy is resolved from the authenticated user — do NOT send it in the body. " +
            "Roles: ADMIN, CLERK, LAWYER."
    )
    public ResponseEntity<ReportResponse> generateReport(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @Valid @RequestBody ReportRequest request) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "LAWYER");
        roleGuard.requireUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.generateReport(request, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a report by ID")
    public ResponseEntity<ReportResponse> getReportById(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "LAWYER");
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    @GetMapping("/me")
    @Operation(summary = "Get all reports requested by the current user")
    public ResponseEntity<List<ReportResponse>> getMyReports(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "LAWYER");
        roleGuard.requireUserId(userId);
        return ResponseEntity.ok(reportService.getReportsByUser(userId));
    }

    // Regex `:.+` prevents Spring MVC from truncating user IDs that contain dots
    // (e.g. emails like "admin@example.com" would otherwise lose ".com")
    @GetMapping("/admin/{adminId:.+}")
    @Operation(summary = "Get all reports requested by a specific user (admin lookup)")
    public ResponseEntity<List<ReportResponse>> getByUser(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String adminId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(reportService.getReportsByUser(adminId));
    }

    @GetMapping("/scope/{scope}")
    @Operation(summary = "Get all reports for a specific scope")
    public ResponseEntity<List<ReportResponse>> getByScope(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable ReportScope scope) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "LAWYER");
        return ResponseEntity.ok(reportService.getReportsByScope(scope));
    }

    @GetMapping("/clerk/{clerkId:.+}")
    @Operation(summary = "Get all reports scoped to a specific clerk")
    public ResponseEntity<List<ReportResponse>> getByClerk(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String clerkId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK");
        return ResponseEntity.ok(reportService.getReportsByScopeAndValue(ReportScope.CLERK, clerkId));
    }

    @GetMapping("/lawyer/{lawyerId:.+}")
    @Operation(summary = "Get all reports scoped to a specific lawyer")
    public ResponseEntity<List<ReportResponse>> getByLawyer(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable String lawyerId) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "LAWYER");
        return ResponseEntity.ok(reportService.getReportsByScopeAndValue(ReportScope.LAWYER, lawyerId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a generated report", description = "Permanently removes a report. Roles: ADMIN.")
    public ResponseEntity<Void> deleteReport(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {
        roleGuard.requireAnyRole(userRole, "ADMIN");
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
