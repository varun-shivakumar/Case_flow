package com.caseflow.compliance.service;

import com.caseflow.compliance.dto.AuditResponse;
import com.caseflow.compliance.dto.ComplianceRecordResponse;
import com.caseflow.compliance.entity.Audit;
import com.caseflow.compliance.entity.ComplianceRecord;
import com.caseflow.compliance.repository.AuditRepository;
import com.caseflow.compliance.repository.ComplianceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedComplianceService {

    private final ComplianceRecordRepository complianceRecordRepository;
    private final AuditRepository auditRepository;

    public Page<ComplianceRecordResponse> getAllComplianceRecordsPaginated(Pageable pageable) {
        return complianceRecordRepository.findAll(pageable).map(this::toComplianceResponse);
    }

    public Page<AuditResponse> getAllAuditsPaginated(Pageable pageable) {
        return auditRepository.findAll(pageable).map(this::toAuditResponse);
    }

    private ComplianceRecordResponse toComplianceResponse(ComplianceRecord r) {
        return ComplianceRecordResponse.builder()
                .complianceId(r.getComplianceId())
                .caseId(r.getCaseId())
                .type(r.getType())
                .result(r.getResult())
                .date(r.getDate())
                .notes(r.getNotes())
                .build();
    }

    private AuditResponse toAuditResponse(Audit a) {
        return AuditResponse.builder()
                .auditId(a.getAuditId())
                .adminId(a.getAdminId())
                .scope(a.getScope())
                .findings(a.getFindings())
                .date(a.getDate())
                .status(a.getStatus())
                .build();
    }
}
