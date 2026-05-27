package com.caseflow.reporting.controller;

import com.caseflow.reporting.dto.ReportResponse;
import com.caseflow.reporting.security.RoleGuard;
import com.caseflow.reporting.service.CachedReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/paginated")
@RequiredArgsConstructor
@Tag(name = "Reports — Paginated", description = "Paginated report endpoints")
public class PaginatedReportController {

    private final CachedReportService cachedReportService;
    private final RoleGuard roleGuard;

    @GetMapping
    @Operation(summary = "Get all reports (paginated)")
    public ResponseEntity<Page<ReportResponse>> getAllReportsPaginated(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reportId,desc") String sort) {
        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "LAWYER");

        String[] s = sort.split(",");
        Sort.Direction direction =
                s.length > 1 && s[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return ResponseEntity.ok(cachedReportService.getAllReportsPaginated(
                PageRequest.of(page, size, Sort.by(direction, s[0]))));
    }
}
