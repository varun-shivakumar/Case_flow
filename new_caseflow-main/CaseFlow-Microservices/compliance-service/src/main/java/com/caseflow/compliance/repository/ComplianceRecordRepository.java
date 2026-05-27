package com.caseflow.compliance.repository;

import com.caseflow.compliance.entity.ComplianceRecord;
import com.caseflow.compliance.entity.ComplianceRecord.ComplianceResult;
import com.caseflow.compliance.entity.ComplianceRecord.ComplianceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, Long> {
    List<ComplianceRecord> findByCaseId(Long caseId);
    List<ComplianceRecord> findByResult(ComplianceResult result);
    List<ComplianceRecord> findByCaseIdAndType(Long caseId, ComplianceType type);
    long countByResult(ComplianceResult result);
}
