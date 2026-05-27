package com.caseflow.appeals.service;

import com.caseflow.appeals.client.CaseServiceClient;
import com.caseflow.appeals.client.IamServiceClient;
import com.caseflow.appeals.client.WorkflowServiceClient;
import com.caseflow.appeals.dto.request.DecisionRequest;
import com.caseflow.appeals.dto.response.CaseOwnerInfo;
import com.caseflow.appeals.dto.response.ReviewResponse;
import com.caseflow.appeals.entity.Appeal;
import com.caseflow.appeals.entity.Appeal.AppealStatus;
import com.caseflow.appeals.entity.AppealAudit.Action;
import com.caseflow.appeals.entity.Review;
import com.caseflow.appeals.entity.Review.ReviewOutcome;
import com.caseflow.appeals.event.AppealEvent;
import com.caseflow.appeals.exception.DuplicateResourceException;
import com.caseflow.appeals.exception.InvalidOperationException;
import com.caseflow.appeals.exception.ResourceNotFoundException;
import com.caseflow.appeals.repository.AppealRepository;
import com.caseflow.appeals.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Handles all review-related business logic:
 * - Assigning a judge and opening a review (SUBMITTED → REVIEWED)
 * - Issuing the final decision           (REVIEWED → DECIDED)
 * - Draft outcome update
 * - All review read operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private static final String ROLE_ADMIN  = "ADMIN";
    private static final String ROLE_JUDGE  = "JUDGE";

    private static final String CASE_STATUS_ACTIVE = "ACTIVE";
    private static final String CASE_STATUS_FILED  = "FILED";
    private static final String FALLBACK_MARKER    = "FALLBACK";

    private final AppealRepository          appealRepository;
    private final ReviewRepository          reviewRepository;
    private final CaseServiceClient         caseClient;
    private final IamServiceClient          iamClient;
    private final WorkflowServiceClient     workflowClient;
    private final ApplicationEventPublisher events;
    private final AppealAuditService        audit;

    // ─── Open For Review ─────────────────────────────────────────────────────

    /**
     * Assign a judge and transition the appeal SUBMITTED → REVIEWED.
     * - The supplied judgeId must exist in iam-service and have the JUDGE role.
     * - The same judge cannot be assigned to two ACTIVE / DECIDED appeals on
     *   the same case (conflict-of-interest guard, ignores CANCELLED appeals).
     */
    @Transactional
    public ReviewResponse openForReview(Long appealId, String judgeId,
                                        String actorUserId, String actorRole) {
        if (judgeId == null || judgeId.isBlank()) {
            throw new InvalidOperationException("judgeId is required to open an appeal for review.");
        }

        Appeal appeal = findAppealOrThrow(appealId);

        if (appeal.getStatus() != AppealStatus.SUBMITTED) {
            throw new InvalidOperationException(
                "Appeal #" + appealId + " must be in SUBMITTED state to open for review. " +
                "Current state: " + appeal.getStatus());
        }

        validateJudge(judgeId);

        if (reviewRepository.existsActiveAssignmentForJudge(appeal.getCaseId(), judgeId)) {
            throw new DuplicateResourceException(
                "Judge [" + judgeId + "] is already (or has been) assigned to an active appeal for case #"
                + appeal.getCaseId() + ". Assign a different judge to avoid conflict of interest.");
        }

        appeal.setStatus(AppealStatus.REVIEWED);
        appealRepository.save(appeal);

        Review review = Review.builder()
            .caseId(appeal.getCaseId())
            .appealId(appealId)
            .judgeId(judgeId)
            .assignedByClerkId(actorUserId)
            .reviewDate(LocalDateTime.now())
            .build();
        review = reviewRepository.save(review);
        log.info("Appeal #{} opened for review. Assigned to judge [{}]", appealId, judgeId);

        audit.record(appealId, Action.OPENED_FOR_REVIEW,
            actorUserId, actorRole,
            AppealStatus.SUBMITTED, AppealStatus.REVIEWED,
            "judgeId=" + judgeId);

        CaseOwnerInfo caseInfo = caseClient.getCaseDetails(appeal.getCaseId());
        events.publishEvent(new AppealEvent(
            AppealEvent.Type.UNDER_REVIEW,
            appealId,
            appeal.getCaseId(),
            AppealService.stakeholders(appeal.getFiledByUserId(), caseInfo, judgeId),
            "Appeal #" + appealId + " for case #" + appeal.getCaseId()
                + " is now under review by judge [" + judgeId + "]."
        ));

        return toReviewResponse(review);
    }

    private void validateJudge(String judgeId) {
        Boolean exists;
        String  role;
        try {
            exists = iamClient.existsById(judgeId);
            role   = iamClient.getUserRole(judgeId);
        } catch (Exception e) {
            log.error("Unable to reach iam-service to validate judge [{}]: {}", judgeId, e.getMessage());
            throw new InvalidOperationException(
                "Cannot validate judge — IAM service is temporarily unavailable. Please try again.");
        }
        if (exists == null || role == null) {
            throw new InvalidOperationException(
                "Cannot validate judge — IAM service is temporarily unavailable. Please try again.");
        }
        if (!Boolean.TRUE.equals(exists)) {
            throw new ResourceNotFoundException("Judge not found in IAM: [" + judgeId + "]");
        }
        if (!ROLE_JUDGE.equalsIgnoreCase(role)) {
            throw new InvalidOperationException(
                "User [" + judgeId + "] cannot be assigned as a judge. Their role is " + role + ".");
        }
    }

    // ─── Issue Decision ──────────────────────────────────────────────────────

    /**
     * Issue the final decision on a REVIEWED appeal: REVIEWED → DECIDED.
     * Only the originally assigned judge or an ADMIN can decide.
     * Case status is updated via case-service based on the outcome.
     * The DECIDED status is always persisted even if downstream calls fail.
     */
    @Transactional
    public ReviewResponse issueDecision(Long appealId, String judgeId,
                                        DecisionRequest request, String userRole) {
        Appeal appeal = findAppealOrThrow(appealId);
        if (appeal.getStatus() != AppealStatus.REVIEWED) {
            throw new InvalidOperationException(
                "Decision can only be issued on a REVIEWED appeal. Current state: " + appeal.getStatus());
        }

        Review review = reviewRepository.findByAppealId(appealId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No review record found for appeal #" + appealId));

        if (!ROLE_ADMIN.equalsIgnoreCase(userRole) && !judgeId.equals(review.getJudgeId())) {
            throw new InvalidOperationException(
                "Access denied: Only the assigned judge [" + review.getJudgeId() + "] " +
                "or an ADMIN can issue a decision on appeal #" + appealId + ".");
        }

        review.setOutcome(request.getOutcome());
        review.setRemarks(request.getRemarks());
        reviewRepository.save(review);

        appeal.setStatus(AppealStatus.DECIDED);
        appealRepository.save(appeal);

        ReviewOutcome outcome = request.getOutcome();
        log.info("Decision issued for appeal #{} by judge [{}]: outcome={}", appealId, judgeId, outcome);

        audit.record(appealId, Action.DECIDED,
            judgeId, userRole,
            AppealStatus.REVIEWED, AppealStatus.DECIDED,
            "outcome=" + outcome.name() + ";judgeId=" + review.getJudgeId());

        // Cross-service case-status update — try-catch ensures DECIDED always persists.
        // NOTE: a partial failure here leaves the case stuck CLOSED while the appeal
        // is DECIDED. A future outbox/saga pattern should reconcile this.
        try {
            switch (outcome) {
                case APPROVED -> {
                    log.info("Updating case #{} status: CLOSED → ACTIVE", appeal.getCaseId());
                    Map<String, Object> resp = caseClient.updateCaseStatusInternal(appeal.getCaseId(), CASE_STATUS_ACTIVE);
                    if (resp != null && FALLBACK_MARKER.equals(resp.get("status"))) {
                        log.error("FALLBACK: case-service unavailable — case #{} NOT updated to ACTIVE", appeal.getCaseId());
                    } else {
                        log.info("Case #{} status updated to ACTIVE successfully", appeal.getCaseId());
                    }
                    workflowClient.advanceWorkflow(appeal.getCaseId());
                }
                default ->
                    log.info("Outcome {} — case #{} status unchanged (remains CLOSED)", outcome, appeal.getCaseId());
            }
        } catch (Exception e) {
            log.error("DOWNSTREAM FAILURE: could not update case #{} status after decision [{}] on appeal #{}. " +
                      "Appeal is DECIDED. Cause: {}", appeal.getCaseId(), outcome, appealId, e.getMessage());
        }

        CaseOwnerInfo caseInfo = caseClient.getCaseDetails(appeal.getCaseId());
        events.publishEvent(new AppealEvent(
            AppealEvent.Type.DECIDED,
            appealId,
            appeal.getCaseId(),
            AppealService.stakeholders(appeal.getFiledByUserId(), caseInfo, review.getJudgeId()),
            "Appeal #" + appealId + " for case #" + appeal.getCaseId()
                + " has been decided. Outcome: " + outcome.name()
        ));

        return toReviewResponse(review);
    }

    // ─── Update Draft Outcome ────────────────────────────────────────────────

    /**
     * Update the draft outcome while the appeal is still REVIEWED.
     * Does NOT transition the appeal to DECIDED — call issueDecision() for that.
     */
    @Transactional
    public ReviewResponse updateReviewOutcome(Long reviewId, ReviewOutcome outcome,
                                              String actorUserId, String actorRole) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found: #" + reviewId));

        Appeal appeal = findAppealOrThrow(review.getAppealId());
        if (appeal.getStatus() != AppealStatus.REVIEWED) {
            throw new InvalidOperationException(
                "Cannot update outcome — appeal #" + review.getAppealId()
                + " is not in REVIEWED state. Current state: " + appeal.getStatus());
        }

        ReviewOutcome prior = review.getOutcome();
        review.setOutcome(outcome);
        Review saved = reviewRepository.save(review);

        audit.record(review.getAppealId(), Action.OUTCOME_DRAFT_UPDATED,
            actorUserId, actorRole,
            AppealStatus.REVIEWED, AppealStatus.REVIEWED,
            "previousOutcome=" + prior + ";newOutcome=" + outcome);

        return toReviewResponse(saved);
    }

    // ─── Read Operations ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReviewResponse getReviewByAppeal(Long appealId) {
        return toReviewResponse(
            reviewRepository.findByAppealId(appealId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No review found for appeal #" + appealId)));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByCase(Long caseId) {
        return reviewRepository.findByCaseId(caseId)
            .stream().map(this::toReviewResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByJudge(String judgeId) {
        return reviewRepository.findByJudgeId(judgeId)
            .stream().map(this::toReviewResponse).toList();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Appeal findAppealOrThrow(Long id) {
        return appealRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appeal not found: #" + id));
    }

    ReviewResponse toReviewResponse(Review r) {
        return ReviewResponse.builder()
            .reviewId(r.getReviewId())
            .caseId(r.getCaseId())
            .appealId(r.getAppealId())
            .judgeId(r.getJudgeId())
            .assignedByClerkId(r.getAssignedByClerkId())
            .reviewDate(r.getReviewDate())
            .outcome(r.getOutcome())
            .remarks(r.getRemarks())
            .build();
    }
}
