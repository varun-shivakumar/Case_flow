package com.caseflow.reporting.repository;

import com.caseflow.reporting.entity.Report;
import com.caseflow.reporting.entity.Report.ReportScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByScope(ReportScope scope);
    List<Report> findByScopeAndScopeValue(ReportScope scope, String scopeValue);
    List<Report> findByScopeOrderByGeneratedDateDesc(ReportScope scope);
    List<Report> findByRequestedByOrderByGeneratedDateDesc(String userId);
}
