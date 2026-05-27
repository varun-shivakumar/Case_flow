package com.caseflow.reporting.dto;

import com.caseflow.reporting.entity.Report.ReportScope;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ReportResponse {
    private Long reportId;
    private ReportScope scope;
    private String scopeValue;
    private String metrics;
    private LocalDate generatedDate;
    private String requestedBy;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
