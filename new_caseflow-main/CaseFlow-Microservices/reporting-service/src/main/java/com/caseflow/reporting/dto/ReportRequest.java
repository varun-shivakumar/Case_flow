package com.caseflow.reporting.dto;

import com.caseflow.reporting.entity.Report.ReportScope;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportRequest {

    @NotNull
    private ReportScope scope;

    /**
     * Context for the chosen scope.
     *  - COURT      → "ALL"  (or any short label)
     *  - JUDGE      → judge user-id
     *  - PERIOD     → label e.g. "Q1 2026" — actual filtering uses dateFrom/dateTo
     *  - CLERK      → clerk user-id
     *  - LAWYER     → lawyer user-id
     *  - CASE       → numeric case-id
     *  - COMPLIANCE → "ALL" (or label)
     */
    private String scopeValue;

    /** Optional date filter — only used when scope = PERIOD (or to narrow other scopes). */
    private LocalDate dateFrom;

    /** Optional date filter — only used when scope = PERIOD (or to narrow other scopes). */
    private LocalDate dateTo;
}
