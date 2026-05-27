package com.caseflow.compliance.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per compliance-check run in the Compliance History.
 * Aggregates the per-case ComplianceRecord rows that share the same runId.
 */
@Data
@Builder
public class ComplianceRunSummary {
    /** UUID of the run, or "date-YYYY-MM-DD" for pre-upgrade records that have no runId. */
    private String runId;
    /** True when the runId is a synthetic date-based fallback. */
    private boolean legacy;
    /** Precise instant the run was started. Null for legacy entries. */
    private LocalDateTime runDate;
    /** Date of the records (always present even when runDate is null). */
    private LocalDate date;
    /** Number of distinct cases checked in this run. */
    private int cases;
    /** Total number of compliance records produced (typically 2 × cases). */
    private int checks;
    private int passes;
    private int fails;
    private int documentFails;
    private int processFails;
}
