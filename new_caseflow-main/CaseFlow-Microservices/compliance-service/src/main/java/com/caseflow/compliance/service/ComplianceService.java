package com.caseflow.compliance.service;

import com.caseflow.compliance.dto.AuditRequest;
import com.caseflow.compliance.dto.AuditResponse;
import com.caseflow.compliance.dto.ComplianceCheckRequest;
import com.caseflow.compliance.dto.ComplianceRecordResponse;

import java.util.List;

public interface ComplianceService {

    List<ComplianceRecordResponse> runComplianceCheck(ComplianceCheckRequest request, String userId);

    List<ComplianceRecordResponse> getComplianceRecordsByCase(Long caseId);

    AuditResponse createAudit(AuditRequest request, String userId);

    AuditResponse updateFindings(Long auditId, String findings);

    AuditResponse closeAudit(Long auditId, String userId);

    AuditResponse getAuditById(Long auditId);

    List<AuditResponse> getAuditsByAdmin(String adminId);

    void deleteComplianceRecord(Long complianceId);

    void deleteAudit(Long auditId);

    /** Bulk-delete compliance records by id. Returns count actually deleted. */
    long deleteComplianceRecords(java.util.List<Long> complianceIds);

    /** Bulk-delete audits by id. Returns count actually deleted. */
    long deleteAudits(java.util.List<Long> auditIds);

    /** Returns aggregated runs (one entry per compliance-check invocation). */
    List<com.caseflow.compliance.dto.ComplianceRunSummary> getAllRuns();

    /** Returns the per-case compliance records that make up a single run. */
    List<ComplianceRecordResponse> getRunRecords(String runId);
}
