package com.caseflow.reporting.service;

import com.caseflow.reporting.dto.ReportRequest;
import com.caseflow.reporting.dto.ReportResponse;
import com.caseflow.reporting.entity.Report;

import java.util.List;

public interface ReportService {
    ReportResponse generateReport(ReportRequest request, String requestedBy);
    ReportResponse getReportById(Long id);
    List<ReportResponse> getReportsByUser(String userId);
    List<ReportResponse> getReportsByScope(Report.ReportScope scope);
    List<ReportResponse> getReportsByScopeAndValue(Report.ReportScope scope, String scopeValue);

    void deleteReport(Long reportId);
}
