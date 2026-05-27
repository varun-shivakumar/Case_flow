package com.caseflow.appeals.controller;

import com.caseflow.appeals.dto.response.AppealResponse;
import com.caseflow.appeals.dto.response.ReviewResponse;
import com.caseflow.appeals.service.CachedAppealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Paginated read-only listings of appeals and reviews.
 * Restricted to administrative roles — these endpoints expose system-wide data.
 */
@RestController
@RequestMapping("/api/appeals/paginated")
@RequiredArgsConstructor
@Tag(name = "Appeals — Paginated", description = "Paginated, system-wide appeal listings (admin only)")
public class PaginatedAppealController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_APPEAL_SORT_FIELDS =
        Set.of("appealId", "caseId", "filedDate", "status", "filedByUserId");
    private static final Set<String> ALLOWED_REVIEW_SORT_FIELDS =
        Set.of("reviewId", "caseId", "appealId", "judgeId", "reviewDate", "outcome");

    private final CachedAppealService cachedAppealService;
    private final RoleGuard           roleGuard;

    @GetMapping
    @Operation(summary = "Get all appeals (paginated)")
    public ResponseEntity<Page<AppealResponse>> getAllAppealsPaginated(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appealId,desc") String sort) {

        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE");
        return ResponseEntity.ok(cachedAppealService.getAllAppealsPaginated(
            buildPageable(page, size, sort, ALLOWED_APPEAL_SORT_FIELDS, "appealId")));
    }

    @GetMapping("/reviews")
    @Operation(summary = "Get all reviews (paginated)")
    public ResponseEntity<Page<ReviewResponse>> getAllReviewsPaginated(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reviewId,desc") String sort) {

        roleGuard.requireAnyRole(userRole, "ADMIN", "CLERK", "JUDGE");
        return ResponseEntity.ok(cachedAppealService.getAllReviewsPaginated(
            buildPageable(page, size, sort, ALLOWED_REVIEW_SORT_FIELDS, "reviewId")));
    }

    private PageRequest buildPageable(int page, int size, String sort,
                                       Set<String> allowedFields, String defaultField) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        String[] parts = sort.split(",");
        String field = parts.length > 0 && allowedFields.contains(parts[0]) ? parts[0] : defaultField;
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
            ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}
