package com.caseflow.appeals.controller;

import com.caseflow.appeals.dto.request.DecisionRequest;
import com.caseflow.appeals.dto.request.UpdateOutcomeRequest;
import com.caseflow.appeals.dto.response.ReviewResponse;
import com.caseflow.appeals.entity.Review.ReviewOutcome;
import com.caseflow.appeals.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles all review-level HTTP endpoints.
 * Appeal endpoints live in {@link AppealController}.
 *
 * Review workflow: assign judge → draft outcome (optional) → issue final decision
 */
@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Assign judges, draft outcomes, and issue final decisions on appeals")
public class ReviewController {

    private final ReviewService reviewService;
    private final RoleGuard     roleGuard;

    // ─── Open For Review ──────────────────────────────────────────────────────

    @Operation(
        summary     = "Open appeal for review (assign a judge)",
        description = "Transitions the appeal SUBMITTED → REVIEWED and records the assigned judge.\n" +
                      "The same judge cannot be assigned twice to the same case (conflict-of-interest guard).\n" +
                      "Allowed roles: CLERK, ADMIN, JUDGE."
    )
    @PostMapping("/{id}/review")
    public ResponseEntity<ReviewResponse> openForReview(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id,
            @Parameter(description = "Judge user ID to assign (IAM format, e.g. JOH_JUDGE_1)")
            @RequestParam String judgeId) {

        roleGuard.requireAnyRole(userRole, "CLERK", "ADMIN", "JUDGE");
        roleGuard.requireUserId(currentUserId);
        return ResponseEntity.ok(reviewService.openForReview(id, judgeId, currentUserId, userRole));
    }

    // ─── Get Review ───────────────────────────────────────────────────────────

    @Operation(summary = "Get review details for an appeal")
    @GetMapping("/{id}/review")
    public ResponseEntity<ReviewResponse> getReviewByAppeal(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "JUDGE", "CLERK", "ADMIN");
        return ResponseEntity.ok(reviewService.getReviewByAppeal(id));
    }

    // ─── Issue Decision ───────────────────────────────────────────────────────

    @Operation(
        summary     = "Issue a final decision on an appeal",
        description = "Transitions the appeal REVIEWED → DECIDED.\n" +
                      "Only the judge originally assigned to this review (or ADMIN) can decide.\n" +
                      "Case status is automatically updated based on the outcome:\n" +
                      "  APPEAL_UPHELD / RETRIAL_ORDERED → ACTIVE\n" +
                      "  REMANDED                        → FILED\n" +
                      "  APPEAL_DISMISSED / PARTIALLY_UPHELD → unchanged (CLOSED)"
    )
    @PostMapping("/{id}/decide")
    public ResponseEntity<ReviewResponse> issueDecision(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request) {

        roleGuard.requireAnyRole(userRole, "ADMIN", "JUDGE");
        roleGuard.requireUserId(currentUserId);
        // judgeId is taken from the JWT (X-Auth-User-Id) — not a loose request param
        return ResponseEntity.ok(reviewService.issueDecision(id, currentUserId, request, userRole));
    }

    // ─── Query Reviews ────────────────────────────────────────────────────────

    @Operation(summary = "Get all reviews for a case")
    @GetMapping("/reviews/case/{caseId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByCase(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long caseId) {

        roleGuard.requireAnyRole(userRole, "JUDGE", "CLERK", "ADMIN");
        return ResponseEntity.ok(reviewService.getReviewsByCase(caseId));
    }

    @Operation(summary = "Get all reviews assigned to a specific judge")
    @GetMapping("/reviews/judge/{judgeId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByJudge(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @Parameter(description = "Judge user ID (IAM format, e.g. JOH_JUDGE_1)")
            @PathVariable String judgeId) {

        roleGuard.requireAnyRole(userRole, "JUDGE", "CLERK", "ADMIN");
        return ResponseEntity.ok(reviewService.getReviewsByJudge(judgeId));
    }

    @Operation(
        summary     = "Get my assigned reviews",
        description = "Returns all reviews assigned to the currently logged-in judge (auto-resolved from JWT)."
    )
    @GetMapping("/reviews/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {

        roleGuard.requireAnyRole(userRole, "JUDGE", "ADMIN");
        roleGuard.requireUserId(currentUserId);
        return ResponseEntity.ok(reviewService.getReviewsByJudge(currentUserId));
    }

    // ─── Update Draft Outcome ─────────────────────────────────────────────────

    @Operation(
        summary     = "Update the draft outcome of a review",
        description = "Updates the outcome field while the appeal is still REVIEWED.\n" +
                      "Does NOT transition the appeal to DECIDED.\n" +
                      "Use POST /{id}/decide to issue the final decision."
    )
    @PatchMapping("/reviews/{reviewId}/outcome")
    public ResponseEntity<ReviewResponse> updateReviewOutcome(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateOutcomeRequest request) {

        roleGuard.requireAnyRole(userRole, "ADMIN", "JUDGE");
        roleGuard.requireUserId(currentUserId);
        return ResponseEntity.ok(reviewService.updateReviewOutcome(
            reviewId, request.getOutcome(), currentUserId, userRole));
    }
}
