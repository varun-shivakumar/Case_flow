package com.caseflow.reporting.service;

import com.caseflow.reporting.client.AppealServiceClient;
import com.caseflow.reporting.client.CaseServiceClient;
import com.caseflow.reporting.client.ComplianceServiceClient;
import com.caseflow.reporting.client.HearingServiceClient;
import com.caseflow.reporting.client.WorkflowServiceClient;
import com.caseflow.reporting.client.dto.AppealDto;
import com.caseflow.reporting.client.dto.CaseDto;
import com.caseflow.reporting.client.dto.ComplianceRecordDto;
import com.caseflow.reporting.client.dto.DocumentDto;
import com.caseflow.reporting.client.dto.HearingDto;
import com.caseflow.reporting.client.dto.PageResponse;
import com.caseflow.reporting.client.dto.ReviewDto;
import com.caseflow.reporting.client.dto.SLARecordDto;
import com.caseflow.reporting.dto.ReportRequest;
import com.caseflow.reporting.dto.ReportResponse;
import com.caseflow.reporting.entity.Report;
import com.caseflow.reporting.entity.Report.ReportScope;
import com.caseflow.reporting.exception.BadRequestException;
import com.caseflow.reporting.exception.ResourceNotFoundException;
import com.caseflow.reporting.repository.ReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Generates analytics reports by aggregating REAL data from other microservices.
 *
 * Replaces the previous deterministic-hash placeholder implementation. Uses Feign
 * clients (with fallbacks) so a downstream service outage degrades gracefully:
 * the metric for that area shows zeros instead of failing the whole report.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final CaseServiceClient caseClient;
    private final HearingServiceClient hearingClient;
    private final WorkflowServiceClient workflowClient;
    private final AppealServiceClient appealClient;
    private final ComplianceServiceClient complianceClient;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final ExecutorService IO_POOL = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public ReportResponse generateReport(ReportRequest request, String requestedBy) {
        validateRequest(request, requestedBy);

        String normalizedScopeValue = request.getScopeValue() == null
                ? "ALL"
                : request.getScopeValue().trim();
        if (normalizedScopeValue.isBlank()) normalizedScopeValue = "ALL";

        Map<String, Object> metrics = aggregateMetrics(
                request.getScope(), normalizedScopeValue, request.getDateFrom(), request.getDateTo());

        String metricsJson;
        try {
            metricsJson = MAPPER.writeValueAsString(metrics);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to serialize metrics: " + e.getMessage());
        }

        Report report = Report.builder()
                .scope(request.getScope())
                .scopeValue(normalizedScopeValue)
                .metrics(metricsJson)
                .generatedDate(LocalDate.now())
                .requestedBy(requestedBy)
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .build();

        Report saved = reportRepository.save(report);
        log.info("Generated {} report #{} for scopeValue={} by {}",
                saved.getScope(), saved.getReportId(), saved.getScopeValue(), saved.getRequestedBy());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long id) {
        return toResponse(reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: #" + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByUser(String userId) {
        return reportRepository.findByRequestedByOrderByGeneratedDateDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByScope(ReportScope scope) {
        return reportRepository.findByScopeOrderByGeneratedDateDesc(scope)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByScopeAndValue(ReportScope scope, String scopeValue) {
        if (scopeValue == null || scopeValue.isBlank()) {
            throw new BadRequestException("scopeValue must not be blank");
        }
        return reportRepository.findByScopeAndScopeValue(scope, scopeValue.trim())
                .stream().map(this::toResponse).toList();
    }

    @Override
    public void deleteReport(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Report not found: #" + reportId);
        }
        reportRepository.deleteById(reportId);
        log.info("Deleted report #{}", reportId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aggregation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * "Narrowing" scopes filter strictly to a specific entity (judge / lawyer / single case).
     * If the case set is empty for a narrowing scope, downstream data must also be empty —
     * we must NOT fall back to system-wide queries.
     *
     * "System-wide" scopes (COURT / PERIOD / CLERK / COMPLIANCE) include all data unless
     * a date range filter narrows it.
     */
    private boolean isNarrowingScope(ReportScope scope) {
        return scope == ReportScope.JUDGE
            || scope == ReportScope.LAWYER
            || scope == ReportScope.CASE;
    }

    private Map<String, Object> aggregateMetrics(
            ReportScope scope, String scopeValue, LocalDate dateFrom, LocalDate dateTo) {

        // 1. Determine the case set for this report based on scope + scopeValue
        List<CaseDto> cases = resolveCasesForScope(scope, scopeValue);

        // 2. Apply date range filter (if provided) on filed dates
        if (dateFrom != null || dateTo != null) {
            cases = cases.stream()
                    .filter(c -> withinRange(c.getFiledDate(), dateFrom, dateTo))
                    .toList();
        }

        List<Long> caseIds = cases.stream().map(CaseDto::getCaseId).filter(Objects::nonNull).toList();
        boolean narrowing = isNarrowingScope(scope);

        // Capture request context so the Feign auth-forwarding interceptor works on virtual threads.
        RequestAttributes requestCtx = RequestContextHolder.getRequestAttributes();

        // 3–8. Launch all downstream fetches in parallel — each depends only on caseIds/scope
        // (resolved above) and is independent of the others, so all 6 run concurrently.

        CompletableFuture<List<HearingDto>> allHearingsCF = (narrowing && caseIds.isEmpty())
                ? CompletableFuture.completedFuture(List.of())
                : CompletableFuture.supplyAsync(() -> {
                    if (requestCtx != null) RequestContextHolder.setRequestAttributes(requestCtx);
                    try { return safeList(hearingClient::getAllHearings); }
                    finally { RequestContextHolder.resetRequestAttributes(); }
                }, IO_POOL);

        CompletableFuture<List<DocumentDto>> documentsCF = CompletableFuture.supplyAsync(() -> {
            if (requestCtx != null) RequestContextHolder.setRequestAttributes(requestCtx);
            try {
                List<DocumentDto> docs = new ArrayList<>();
                for (Long caseId : caseIds) {
                    try { docs.addAll(caseClient.getDocumentsByCase(caseId)); }
                    catch (Exception e) { log.warn("Skipping documents for case #{}: {}", caseId, e.getMessage()); }
                }
                return docs;
            } finally { RequestContextHolder.resetRequestAttributes(); }
        }, IO_POOL);

        CompletableFuture<List<SLARecordDto>> slaCF = CompletableFuture.supplyAsync(() -> {
            if (requestCtx != null) RequestContextHolder.setRequestAttributes(requestCtx);
            try {
                List<SLARecordDto> slas = new ArrayList<>();
                if (!caseIds.isEmpty()) {
                    for (Long caseId : caseIds) {
                        try { slas.addAll(workflowClient.getSlaRecordsByCase(caseId)); }
                        catch (Exception e) { log.warn("Skipping SLA for case #{}: {}", caseId, e.getMessage()); }
                    }
                } else if (!narrowing) {
                    slas.addAll(safeList(workflowClient::getActiveSlas));
                    slas.addAll(safeList(workflowClient::getBreachedSlas));
                }
                return slas;
            } finally { RequestContextHolder.resetRequestAttributes(); }
        }, IO_POOL);

        CompletableFuture<List<AppealDto>> allAppealsCF = (narrowing && caseIds.isEmpty())
                ? CompletableFuture.completedFuture(List.of())
                : CompletableFuture.supplyAsync(() -> {
                    if (requestCtx != null) RequestContextHolder.setRequestAttributes(requestCtx);
                    try {
                        PageResponse<AppealDto> pg = appealClient.getAppealsPaginated(0, 5000);
                        return (pg != null && pg.getContent() != null) ? pg.getContent() : List.of();
                    } catch (Exception e) {
                        log.warn("Could not fetch paginated appeals: {}", e.getMessage());
                        return List.<AppealDto>of();
                    } finally { RequestContextHolder.resetRequestAttributes(); }
                }, IO_POOL);

        CompletableFuture<List<ReviewDto>> reviewsCF = CompletableFuture.supplyAsync(() -> {
            if (requestCtx != null) RequestContextHolder.setRequestAttributes(requestCtx);
            try {
                List<ReviewDto> rvs = new ArrayList<>();
                if (scope == ReportScope.JUDGE && scopeValue != null && !scopeValue.isBlank()
                        && !"ALL".equalsIgnoreCase(scopeValue)) {
                    rvs.addAll(safeList(() -> appealClient.getReviewsByJudge(scopeValue)));
                } else if (!caseIds.isEmpty()) {
                    for (Long caseId : caseIds) {
                        try { rvs.addAll(appealClient.getReviewsByCase(caseId)); }
                        catch (Exception e) { log.warn("Skipping reviews for case #{}: {}", caseId, e.getMessage()); }
                    }
                }
                return rvs;
            } finally { RequestContextHolder.resetRequestAttributes(); }
        }, IO_POOL);

        CompletableFuture<List<ComplianceRecordDto>> complianceCF = (narrowing && caseIds.isEmpty())
                ? CompletableFuture.completedFuture(List.of())
                : CompletableFuture.supplyAsync(() -> {
                    if (requestCtx != null) RequestContextHolder.setRequestAttributes(requestCtx);
                    try {
                        PageResponse<ComplianceRecordDto> compPg = complianceClient.getCompliancePaginated(0, 1000);
                        return (compPg != null && compPg.getContent() != null) ? compPg.getContent() : List.of();
                    } catch (Exception e) {
                        log.warn("Could not fetch paginated compliance: {}", e.getMessage());
                        return List.<ComplianceRecordDto>of();
                    } finally { RequestContextHolder.resetRequestAttributes(); }
                }, IO_POOL);

        // Join all parallel fetches
        List<HearingDto> allHearings            = allHearingsCF.join();
        List<DocumentDto> documents             = documentsCF.join();
        List<SLARecordDto> slaRecords           = slaCF.join();
        List<AppealDto> allAppeals              = allAppealsCF.join();
        List<ReviewDto> reviews                 = reviewsCF.join();
        List<ComplianceRecordDto> allCompliance = complianceCF.join();

        // ── Filter hearings ──────────────────────────────────────────────────
        List<HearingDto> hearings = allHearings.stream()
                .filter(h -> caseIds.isEmpty() || caseIds.contains(h.getCaseId()))
                .filter(h -> withinRange(h.getHearingDate(), dateFrom, dateTo))
                .toList();
        if (scope == ReportScope.JUDGE && scopeValue != null && !scopeValue.isBlank()
                && !"ALL".equalsIgnoreCase(scopeValue)) {
            hearings = hearings.stream().filter(h -> scopeValue.equals(h.getJudgeId())).toList();
        }
        if (scope == ReportScope.CLERK && scopeValue != null && !scopeValue.isBlank()
                && !"ALL".equalsIgnoreCase(scopeValue)) {
            hearings = hearings.stream().filter(h -> scopeValue.equals(h.getScheduledBy())).toList();
        }

        // ── Filter documents (CLERK scope) ───────────────────────────────────
        if (scope == ReportScope.CLERK && scopeValue != null && !scopeValue.isBlank()
                && !"ALL".equalsIgnoreCase(scopeValue)) {
            documents = documents.stream().filter(d -> scopeValue.equals(d.getVerifiedBy())).toList();
        }

        // ── Filter appeals ───────────────────────────────────────────────────
        List<AppealDto> appeals;
        if (scope == ReportScope.LAWYER && scopeValue != null && !scopeValue.isBlank()
                && !"ALL".equalsIgnoreCase(scopeValue)) {
            appeals = allAppeals.stream().filter(a -> scopeValue.equals(a.getFiledByUserId())).toList();
        } else if (!caseIds.isEmpty()) {
            java.util.Set<Long> caseIdSet = new java.util.HashSet<>(caseIds);
            appeals = allAppeals.stream()
                    .filter(a -> a.getCaseId() != null && caseIdSet.contains(a.getCaseId()))
                    .toList();
        } else {
            appeals = allAppeals;
        }
        appeals = appeals.stream()
                .filter(a -> withinRange(a.getFiledDate(), dateFrom, dateTo))
                .toList();

        // ── Filter reviews (CLERK scope) ─────────────────────────────────────
        if (scope == ReportScope.CLERK && scopeValue != null && !scopeValue.isBlank()
                && !"ALL".equalsIgnoreCase(scopeValue)) {
            reviews = reviews.stream()
                    .filter(r -> scopeValue.equals(r.getAssignedByClerkId()))
                    .toList();
            java.util.Set<Long> clerkRoutedAppealIds = new java.util.HashSet<>();
            for (ReviewDto rv : reviews) {
                if (rv.getAppealId() != null) clerkRoutedAppealIds.add(rv.getAppealId());
            }
            appeals = appeals.stream()
                    .filter(a -> clerkRoutedAppealIds.contains(a.getAppealId()))
                    .toList();
        }

        // ── Filter compliance ────────────────────────────────────────────────
        List<ComplianceRecordDto> compliance = allCompliance;
        if (!caseIds.isEmpty()) {
            compliance = compliance.stream().filter(c -> caseIds.contains(c.getCaseId())).toList();
        }
        compliance = compliance.stream()
                .filter(c -> withinRange(c.getDate(), dateFrom, dateTo))
                .toList();

        // ── Build metrics map ────────────────────────────────────────────────
        return buildMetrics(scope, scopeValue, dateFrom, dateTo,
                cases, documents, hearings, slaRecords, appeals, reviews, compliance);
    }

    private List<CaseDto> resolveCasesForScope(ReportScope scope, String scopeValue) {
        try {
            switch (scope) {
                case CASE: {
                    Long caseId = parseLongOrNull(scopeValue);
                    if (caseId == null) return List.of();
                    CaseDto c = caseClient.getCaseById(caseId);
                    return c == null ? List.of() : List.of(c);
                }
                case JUDGE: {
                    // The Case entity does NOT carry a judgeId field — judges are linked to
                    // cases only through hearings. So a judge's caseload = the distinct cases
                    // for which they have at least one hearing assignment.
                    if (scopeValue == null || scopeValue.isBlank() || "ALL".equalsIgnoreCase(scopeValue)) {
                        return caseClient.getAllCases();
                    }
                    List<HearingDto> allHearings = safeList(hearingClient::getAllHearings);
                    java.util.Set<Long> judgeCaseIds = new java.util.HashSet<>();
                    for (HearingDto h : allHearings) {
                        if (scopeValue.equals(h.getJudgeId()) && h.getCaseId() != null) {
                            judgeCaseIds.add(h.getCaseId());
                        }
                    }
                    if (judgeCaseIds.isEmpty()) return List.of();
                    return caseClient.getAllCases().stream()
                            .filter(c -> judgeCaseIds.contains(c.getCaseId()))
                            .toList();
                }
                case CLERK: {
                    // No direct "by clerk" — return all cases (clerk works system-wide)
                    return caseClient.getAllCases();
                }
                case LAWYER: {
                    if (scopeValue == null || scopeValue.isBlank() || "ALL".equalsIgnoreCase(scopeValue)) {
                        return caseClient.getAllCases();
                    }
                    return caseClient.getCasesByLawyer(scopeValue);
                }
                case COURT, PERIOD, COMPLIANCE:
                default:
                    return caseClient.getAllCases();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve cases for scope={} value={}: {}", scope, scopeValue, e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> buildMetrics(
            ReportScope scope, String scopeValue, LocalDate dateFrom, LocalDate dateTo,
            List<CaseDto> cases, List<DocumentDto> documents, List<HearingDto> hearings,
            List<SLARecordDto> slaRecords, List<AppealDto> appeals, List<ReviewDto> reviews,
            List<ComplianceRecordDto> compliance) {

        // Cases
        int totalCasesFiled = cases.size();
        int casesActive    = (int) cases.stream().filter(c -> equalsAny(c.getStatus(), "ACTIVE", "FILED", "UNDER_REVIEW", "HEARING_SCHEDULED")).count();
        int casesClosed    = (int) cases.stream().filter(c -> equalsAny(c.getStatus(), "CLOSED", "DECIDED")).count();
        int casesAdjourned = (int) cases.stream().filter(c -> equalsAny(c.getStatus(), "ADJOURNED")).count();
        int casesAppealed  = (int) cases.stream().filter(c -> equalsAny(c.getStatus(), "APPEALED")).count();

        // Documents
        int totalDocuments    = documents.size();
        int documentVerified  = (int) documents.stream().filter(d -> equalsAny(d.getVerificationStatus(), "VERIFIED")).count();
        int documentRejected  = (int) documents.stream().filter(d -> equalsAny(d.getVerificationStatus(), "REJECTED")).count();
        int documentPending   = (int) documents.stream().filter(d -> equalsAny(d.getVerificationStatus(), "PENDING")).count();

        // Hearings
        int totalHearings        = hearings.size();
        int hearingsCompleted    = (int) hearings.stream().filter(h -> equalsAny(h.getStatus(), "COMPLETED")).count();
        int hearingsScheduled    = (int) hearings.stream().filter(h -> equalsAny(h.getStatus(), "SCHEDULED")).count();
        int hearingsRescheduled  = (int) hearings.stream().filter(h -> equalsAny(h.getStatus(), "RESCHEDULED")).count();
        int hearingsCancelled    = (int) hearings.stream().filter(h -> equalsAny(h.getStatus(), "CANCELLED")).count();

        // SLA
        int totalSlaRecords = slaRecords.size();
        int slaBreaches     = (int) slaRecords.stream().filter(s -> equalsAny(s.getStatus(), "BREACHED")).count();
        int slaWarnings     = (int) slaRecords.stream().filter(s -> equalsAny(s.getStatus(), "WARNING")).count();
        int slaActive       = (int) slaRecords.stream().filter(s -> equalsAny(s.getStatus(), "ACTIVE")).count();
        int slaClosed       = (int) slaRecords.stream().filter(s -> equalsAny(s.getStatus(), "CLOSED", "COMPLETED", "ON_TIME")).count();

        // Appeals — must match the actual AppealStatus enum on appeal-service
        // (SUBMITTED, REVIEWED, DECIDED, CANCELLED). The previous values
        // ("FILED", "UNDER_REVIEW", "DECISION_ISSUED", "CLOSED") did not match
        // anything, so COURT / PERIOD / LAWYER / CLERK reports always showed 0.
        int totalAppeals       = appeals.size();
        int appealsFiled       = (int) appeals.stream().filter(a -> equalsAny(a.getStatus(), "SUBMITTED", "FILED")).count();
        int appealsUnderReview = (int) appeals.stream().filter(a -> equalsAny(a.getStatus(), "REVIEWED", "UNDER_REVIEW")).count();
        int appealsDecided     = (int) appeals.stream().filter(a -> equalsAny(a.getStatus(), "DECIDED", "DECISION_ISSUED", "CLOSED")).count();
        // Distinct cases that have at least one appeal — used for the % rate (caps at 100%)
        int casesWithAppeals   = (int) appeals.stream()
                .map(AppealDto::getCaseId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // For JUDGE / CLERK scopes, the appeals metrics should reflect the user's review
        // activity (appeals routed TO a judge / routed BY a clerk). We rebuild the counts
        // from the reviews list which is already filtered by judgeId / assignedByClerkId.
        if ((scope == ReportScope.JUDGE || scope == ReportScope.CLERK) && !reviews.isEmpty()) {
            int reviewsAssigned  = reviews.size();
            int reviewsCompleted = (int) reviews.stream()
                    .filter(r -> r.getOutcome() != null && !r.getOutcome().isBlank())
                    .count();
            int reviewsPending   = reviewsAssigned - reviewsCompleted;
            totalAppeals       = reviewsAssigned;
            appealsFiled       = reviewsAssigned;     // appeals routed (to/by this user)
            appealsUnderReview = reviewsPending;      // still in process
            appealsDecided     = reviewsCompleted;    // review completed
            casesWithAppeals   = (int) reviews.stream()
                    .map(ReviewDto::getCaseId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
        }

        // Review outcomes
        int upheld    = (int) reviews.stream().filter(r -> equalsAny(r.getOutcome(), "APPROVED")).count();
        int dismissed = (int) reviews.stream().filter(r -> equalsAny(r.getOutcome(), "REJECTED")).count();

        // Compliance
        int totalCompliance      = compliance.size();
        int compliancePass       = (int) compliance.stream().filter(c -> equalsAny(c.getResult(), "PASS")).count();
        int complianceFail       = (int) compliance.stream().filter(c -> equalsAny(c.getResult(), "FAIL")).count();
        int complianceDocFails   = (int) compliance.stream().filter(c -> equalsAny(c.getResult(), "FAIL") && equalsAny(c.getType(), "DOCUMENT")).count();
        int complianceProcFails  = (int) compliance.stream().filter(c -> equalsAny(c.getResult(), "FAIL") && equalsAny(c.getType(), "PROCESS")).count();

        // Rates
        double caseClearanceRate         = percentage(casesClosed, totalCasesFiled);
        double documentVerificationRate  = percentage(documentVerified, totalDocuments);
        double documentRejectionRate     = percentage(documentRejected, totalDocuments);
        double hearingCompletionRate     = percentage(hearingsCompleted, totalHearings);
        double slaAdherenceRate          = percentage(totalSlaRecords - slaBreaches, totalSlaRecords);
        // % of cases that have been appealed at least once — naturally capped at 100%
        double appealRate                = percentage(casesWithAppeals, totalCasesFiled);
        // Average number of appeals filed per case (can exceed 1.0 if a case has multiple appeals)
        double appealsPerCase            = totalCasesFiled > 0
                ? java.math.BigDecimal.valueOf((double) totalAppeals / totalCasesFiled)
                        .setScale(2, java.math.RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double compliancePassRate        = percentage(compliancePass, totalCompliance);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("scope", scope.name());
        root.put("scopeValue", scopeValue);
        root.put("dateFrom", dateFrom);
        root.put("dateTo", dateTo);
        root.put("generatedAt", LocalDate.now());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCasesFiled", totalCasesFiled);
        summary.put("casesActive",     casesActive);
        summary.put("casesClosed",     casesClosed);
        summary.put("casesAdjourned",  casesAdjourned);
        summary.put("casesAppealed",   casesAppealed);
        summary.put("caseClearanceRate", caseClearanceRate);
        root.put("summary", summary);

        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("totalDocuments",          totalDocuments);
        docs.put("verifiedDocuments",       documentVerified);
        docs.put("rejectedDocuments",       documentRejected);
        docs.put("pendingDocuments",        documentPending);
        docs.put("documentVerificationRate", documentVerificationRate);
        docs.put("documentRejectionRate",    documentRejectionRate);
        root.put("documents", docs);

        Map<String, Object> hr = new LinkedHashMap<>();
        hr.put("totalHearings",       totalHearings);
        hr.put("hearingsCompleted",   hearingsCompleted);
        hr.put("hearingsScheduled",   hearingsScheduled);
        hr.put("hearingsRescheduled", hearingsRescheduled);
        hr.put("hearingsCancelled",   hearingsCancelled);
        hr.put("hearingCompletionRate", hearingCompletionRate);
        root.put("hearings", hr);

        Map<String, Object> sla = new LinkedHashMap<>();
        sla.put("totalSlaRecords", totalSlaRecords);
        sla.put("slaBreaches",     slaBreaches);
        sla.put("slaWarnings",     slaWarnings);
        sla.put("slaActive",       slaActive);
        sla.put("slaClosed",       slaClosed);
        sla.put("slaAdherenceRate", slaAdherenceRate);
        root.put("sla", sla);

        Map<String, Object> ap = new LinkedHashMap<>();
        ap.put("totalAppeals",       totalAppeals);
        ap.put("appealsFiled",       appealsFiled);
        ap.put("appealsUnderReview", appealsUnderReview);
        ap.put("appealsDecided",     appealsDecided);
        ap.put("casesWithAppeals",   casesWithAppeals);
        ap.put("appealRate",         appealRate);          // % of cases appealed (≤ 100%)
        ap.put("appealsPerCase",     appealsPerCase);      // avg appeals per case (can exceed 1.0)
        Map<String, Object> outcomes = new LinkedHashMap<>();
        outcomes.put("approved",  upheld);
        outcomes.put("rejected",  dismissed);
        ap.put("outcomes", outcomes);
        root.put("appeals", ap);

        Map<String, Object> co = new LinkedHashMap<>();
        co.put("totalComplianceChecks",     totalCompliance);
        co.put("compliancePasses",          compliancePass);
        co.put("complianceFailures",        complianceFail);
        co.put("complianceDocumentFailures", complianceDocFails);
        co.put("complianceProcessFailures",  complianceProcFails);
        co.put("compliancePassRate",        compliancePassRate);
        root.put("compliance", co);

        return root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validateRequest(ReportRequest request, String requestedBy) {
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new BadRequestException("Missing authentication — requestedBy could not be resolved");
        }
        if (request.getScope() == null) {
            throw new BadRequestException("scope must not be null");
        }
        if (request.getDateFrom() != null && request.getDateTo() != null
                && request.getDateFrom().isAfter(request.getDateTo())) {
            throw new BadRequestException("dateFrom must be on or before dateTo");
        }
    }

    private boolean withinRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return true; // don't drop records that lack a date
        if (from != null && date.isBefore(from)) return false;
        if (to   != null && date.isAfter(to))    return false;
        return true;
    }

    private boolean equalsAny(String value, String... candidates) {
        if (value == null) return false;
        for (String c : candidates) {
            if (c.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private double percentage(int value, int total) {
        if (total <= 0) return 0.0;
        return BigDecimal.valueOf((value * 100.0) / total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private <T> List<T> safeList(java.util.function.Supplier<List<T>> call) {
        try {
            List<T> result = call.get();
            return result == null ? List.of() : result;
        } catch (Exception e) {
            log.warn("Downstream call failed: {}", e.getMessage());
            return List.of();
        }
    }

    private ReportResponse toResponse(Report r) {
        return ReportResponse.builder()
                .reportId(r.getReportId())
                .scope(r.getScope())
                .scopeValue(r.getScopeValue())
                .metrics(r.getMetrics())
                .generatedDate(r.getGeneratedDate())
                .requestedBy(r.getRequestedBy())
                .dateFrom(r.getDateFrom())
                .dateTo(r.getDateTo())
                .build();
    }
}
