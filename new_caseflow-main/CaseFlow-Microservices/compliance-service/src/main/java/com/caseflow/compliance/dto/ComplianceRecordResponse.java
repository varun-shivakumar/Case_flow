package com.caseflow.compliance.dto;

import com.caseflow.compliance.entity.ComplianceRecord.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ComplianceRecordResponse {
    private Long complianceId;
    private Long caseId;
    private ComplianceType type;
    private ComplianceResult result;
    private LocalDate date;
    private String notes;
    /** UUID identifying the compliance-check run that produced this record. */
    private String runId;
    /** Precise instant the run was started (date + time). */
    private LocalDateTime runDate;
}
