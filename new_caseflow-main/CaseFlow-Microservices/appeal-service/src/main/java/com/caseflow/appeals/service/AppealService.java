package com.caseflow.appeals.service;

import com.caseflow.appeals.client.CaseServiceClient;
import com.caseflow.appeals.dto.request.AppealRequest;
import com.caseflow.appeals.dto.response.AppealResponse;
import com.caseflow.appeals.dto.response.CaseOwnerInfo;
import com.caseflow.appeals.entity.Appeal;
import com.caseflow.appeals.entity.Appeal.AppealStatus;
import com.caseflow.appeals.entity.AppealAudit.Action;
import com.caseflow.appeals.event.AppealEvent;
import com.caseflow.appeals.exception.DuplicateResourceException;
import com.caseflow.appeals.exception.InvalidOperationException;
import com.caseflow.appeals.exception.ResourceNotFoundException;
import com.caseflow.appeals.repository.AppealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles all appeal-related business logic:
 * - Filing a new appeal
 * - Cancelling an appeal
 * - All appeal read operations
 *
 * Review-related logic lives in {@link ReviewService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppealService {

    /** Case status string returned by case-service that allows appeals. */
    static final String CASE_STATUS_CLOSED = "CLOSED";

    static final String ROLE_ADMIN    = "ADMIN";
    static final String ROLE_LITIGANT = "LITIGANT";
    static final String ROLE_LAWYER   = "LAWYER";

    private final AppealRepository          appealRepository;
    private final CaseServiceClient         caseClient;
    private final ApplicationEventPublisher events;
    private final AppealAuditService        audit;
    private final com.caseflow.appeals.client.IamServiceClient iamClient;

    /** Filing deadline (days after case CLOSED). 0 or negative disables the check. */
    @Value("${appeal.filing-deadline-days:90}")
    private int filingDeadlineDays;

    // ─── File Appeal ─────────────────────────────────────────────────────────

    /**
     * File a new appeal.
     * - filedByUserId is taken from the JWT (X-Auth-User-Id), never from the request body.
     * - Ownership guard: LITIGANT → only their own case,
     *                    LAWYER  → only their assigned case,
     *                    ADMIN   → any case.
     * - Enforces the filing deadline (filing-deadline-days after the case was CLOSED).
     * - Blocks filing if a SUBMITTED or REVIEWED appeal already exists for the case.
     */
    @Transactional
    public AppealResponse fileAppeal(AppealRequest request, String currentUserId, String userRole) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new InvalidOperationException(
                "Unable to identify the current user. Please re-login.");
        }

        CaseOwnerInfo caseInfo = caseClient.getCaseDetails(request.getCaseId());
        if (caseInfo == null) {
            throw new InvalidOperationException(
                "Cannot validate case — case service is temporarily unavailable. Please try again.");
        }

        if (!CASE_STATUS_CLOSED.equals(caseInfo.getStatus())) {
            throw new InvalidOperationException(
                "Appeals can only be filed on " + CASE_STATUS_CLOSED + " cases. " +
                "Current case status: " + caseInfo.getStatus());
        }

        enforceFilingDeadline(request.getCaseId(), caseInfo);

        if (!ROLE_ADMIN.equalsIgnoreCase(userRole)) {
            if (ROLE_LITIGANT.equalsIgnoreCase(userRole)) {
                if (!currentUserId.equals(caseInfo.getLitigantId())) {
                    throw new InvalidOperationException(
                        "Access denied: You can only file an appeal for your own case. " +
                        "Case #" + request.getCaseId() + " belongs to a different litigant.");
                }
            } else if (ROLE_LAWYER.equalsIgnoreCase(userRole)) {
                if (caseInfo.getLawyerId() == null || !currentUserId.equals(caseInfo.getLawyerId())) {
                    throw new InvalidOperationException(
                        "Access denied: You can only file an appeal for cases where you are the assigned lawyer. " +
                        "You are not assigned to case #" + request.getCaseId() + ".");
                }
            }
        }
        log.info("Ownership check passed for case #{} by {} [{}]",
            request.getCaseId(), userRole, currentUserId);

        if (appealRepository.existsByCaseIdAndStatus(request.getCaseId(), AppealStatus.SUBMITTED)
                || appealRepository.existsByCaseIdAndStatus(request.getCaseId(), AppealStatus.REVIEWED)) {
            throw new DuplicateResourceException(
                "An active appeal (SUBMITTED or REVIEWED) already exists for case #"
                + request.getCaseId()
                + ". Wait for it to be decided before filing a new one.");
        }

        Appeal appeal = Appeal.builder()
            .caseId(request.getCaseId())
            .filedByUserId(currentUserId)
            .filedDate(LocalDateTime.now())
            .reason(request.getReason())
            .status(AppealStatus.SUBMITTED)
            .build();
        appeal = appealRepository.save(appeal);
        log.info("Appeal #{} filed for case #{} by [{}]",
            appeal.getAppealId(), appeal.getCaseId(), currentUserId);

        audit.record(appeal.getAppealId(), Action.FILED,
            currentUserId, userRole,
            null, AppealStatus.SUBMITTED,
            "caseId=" + appeal.getCaseId());

        // Per spec: APPEAL FILED → notify Judge, Clerk (and we also keep the filer/litigant/lawyer
        // informed so the case parties know it landed). Since no judge is assigned at filing time,
        // the "Judge" recipient is satisfied later when the review is opened (UNDER_REVIEW event).
        java.util.Set<String> filedRecipients = new java.util.LinkedHashSet<>(stakeholders(currentUserId, caseInfo, null));
        filedRecipients.addAll(usersWithRole("CLERK"));
        events.publishEvent(new AppealEvent(
            AppealEvent.Type.FILED,
            appeal.getAppealId(),
            appeal.getCaseId(),
            filedRecipients,
            "Appeal #" + appeal.getAppealId() + " has been filed for case #" + appeal.getCaseId() + "."
        ));

        return toAppealResponse(appeal);
    }

    /**
     * Reject filings that arrive after the configured deadline. The check is
     * skipped if filingDeadlineDays <= 0 or if case-service did not provide
     * a closedDate (e.g. legacy data filed before the closedDate column existed).
     */
    private void enforceFilingDeadline(Long caseId, CaseOwnerInfo caseInfo) {
        if (filingDeadlineDays <= 0) return;

        LocalDateTime closedAt = caseInfo.getClosedDate();
        if (closedAt == null) {
            log.warn("Case #{} has no closedDate — skipping filing-deadline check (legacy data?)", caseId);
            return;
        }
        LocalDateTime deadline = closedAt.plusDays(filingDeadlineDays);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new InvalidOperationException(
                "The appeal-filing window has closed. Appeals must be filed within "
                + filingDeadlineDays + " days of the case being CLOSED. "
                + "Case #" + caseId + " was closed on " + closedAt + " (deadline: " + deadline + ").");
        }
    }

    // ─── Cancel Appeal ───────────────────────────────────────────────────────

    /**
     * Cancel/withdraw an appeal.
     * - LITIGANT / LAWYER : can cancel only their own SUBMITTED appeal.
     * - ADMIN             : can cancel any SUBMITTED or REVIEWED appeal.
     * - DECIDED / CANCELLED appeals cannot be cancelled.
     * - After cancellation a new appeal CAN be filed for the same case.
     */
    @Transactional
    public AppealResponse cancelAppeal(Long appealId, String currentUserId, String userRole) {
        Appeal appeal = findAppealOrThrow(appealId);

        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(userRole);
        boolean isFiler = currentUserId.equals(appeal.getFiledByUserId());

        if (appeal.getStatus() == AppealStatus.DECIDED) {
            throw new InvalidOperationException(
                "Appeal #" + appealId + " has already been DECIDED and cannot be cancelled.");
        }
        if (appeal.getStatus() == AppealStatus.CANCELLED) {
            throw new InvalidOperationException(
                "Appeal #" + appealId + " is already CANCELLED.");
        }
        if (appeal.getStatus() == AppealStatus.REVIEWED && !isAdmin) {
            throw new InvalidOperationException(
                "Appeal #" + appealId + " is already under review and cannot be withdrawn by the filer. " +
                "Contact an administrator.");
        }
        if (!isAdmin && !isFiler) {
            throw new InvalidOperationException(
                "Access denied: Only the person who filed this appeal or an ADMIN can cancel it.");
        }

        AppealStatus prior = appeal.getStatus();
        appeal.setStatus(AppealStatus.CANCELLED);
        appeal = appealRepository.save(appeal);
        log.info("Appeal #{} cancelled by {} [{}]", appealId, userRole, currentUserId);

        audit.record(appealId, Action.CANCELLED,
            currentUserId, userRole,
            prior, AppealStatus.CANCELLED,
            null);

        // Best-effort lookup of case parties for fan-out notification; if
        // case-service is down we still notify the filer.
        CaseOwnerInfo caseInfo = caseClient.getCaseDetails(appeal.getCaseId());

        events.publishEvent(new AppealEvent(
            AppealEvent.Type.CANCELLED,
            appeal.getAppealId(),
            appeal.getCaseId(),
            stakeholders(appeal.getFiledByUserId(), caseInfo, null),
            "Appeal #" + appealId + " for case #" + appeal.getCaseId() + " has been cancelled."
        ));

        return toAppealResponse(appeal);
    }

    // ─── Read Operations ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AppealResponse getAppealById(Long id) {
        return toAppealResponse(findAppealOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AppealResponse> getAppealsByCase(Long caseId) {
        return appealRepository.findByCaseId(caseId)
            .stream().map(this::toAppealResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AppealResponse> getAppealsByUser(String userId) {
        return appealRepository.findByFiledByUserId(userId)
            .stream().map(this::toAppealResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AppealResponse> getAppealsByStatus(AppealStatus status) {
        return appealRepository.findByStatus(status)
            .stream().map(this::toAppealResponse).toList();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Build the stakeholder set for an appeal event:
     * filer + litigant + lawyer (if any) + judge (if any), deduplicated and
     * null-stripped. Order is preserved for deterministic logging via LinkedHashSet.
     */
    static Set<String> stakeholders(String filerUserId, CaseOwnerInfo caseInfo, String judgeId) {
        Set<String> ids = new LinkedHashSet<>();
        addIfPresent(ids, filerUserId);
        if (caseInfo != null) {
            addIfPresent(ids, caseInfo.getLitigantId());
            addIfPresent(ids, caseInfo.getLawyerId());
        }
        addIfPresent(ids, judgeId);
        return ids;
    }

    /** Resolve every active user with the given role from iam-service. */
    Set<String> usersWithRole(String role) {
        try {
            var users = iamClient.getUsersByRole(role);
            if (users == null) return Set.of();
            Set<String> out = new LinkedHashSet<>();
            for (var u : users) {
                if (u == null || u.getUserId() == null) continue;
                if (u.getStatus() != null && !"ACTIVE".equalsIgnoreCase(u.getStatus())) continue;
                out.add(u.getUserId());
            }
            return out;
        } catch (Exception e) {
            log.warn("Could not resolve users with role {} for notification fan-out: {}", role, e.getMessage());
            return Set.of();
        }
    }

    private static void addIfPresent(Set<String> ids, String id) {
        if (id != null && !id.isBlank()) ids.add(id);
    }

    private Appeal findAppealOrThrow(Long id) {
        return appealRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appeal not found: #" + id));
    }

    AppealResponse toAppealResponse(Appeal a) {
        return AppealResponse.builder()
            .appealId(a.getAppealId())
            .caseId(a.getCaseId())
            .filedByUserId(a.getFiledByUserId())
            .filedDate(a.getFiledDate())
            .reason(a.getReason())
            .status(a.getStatus())
            .build();
    }
}
