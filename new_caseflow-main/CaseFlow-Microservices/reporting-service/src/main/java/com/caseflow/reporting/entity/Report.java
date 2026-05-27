package com.caseflow.reporting.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "reports") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportScope scope;

    @Column(nullable = false) private String scopeValue;

    @Column(nullable = false, columnDefinition = "TEXT") private String metrics;

    @Column(nullable = false) private LocalDate generatedDate;

    /** ID of the user (email) who requested the report — read from JWT/X-Auth-User-Id header. */
    @Column(nullable = false) private String requestedBy;

    /** Optional date range used during generation (only meaningful when scope = PERIOD). */
    private LocalDate dateFrom;
    private LocalDate dateTo;

    /**
     * COURT      — entire system / court-wide report
     * JUDGE      — metrics for one specific judge (scopeValue = judgeId)
     * PERIOD     — metrics for a date range (dateFrom..dateTo); scopeValue is a label
     * CLERK      — workload for one clerk (scopeValue = clerkId)
     * LAWYER     — case outcomes for one lawyer (scopeValue = lawyerId)
     * CASE       — drill-down report for a single case (scopeValue = caseId)
     * COMPLIANCE — compliance-focused report (audit trail + checks)
     */
    public enum ReportScope { COURT, JUDGE, PERIOD, CLERK, LAWYER, CASE, COMPLIANCE }
}
