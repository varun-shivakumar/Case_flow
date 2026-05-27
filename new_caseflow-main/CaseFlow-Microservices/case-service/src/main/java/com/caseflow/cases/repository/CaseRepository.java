package com.caseflow.cases.repository;

import com.caseflow.cases.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaseRepository extends JpaRepository<Case, Long> {
    List<Case> findByLitigantId(String litigantId);
    List<Case> findByLawyerId(String lawyerId);
    List<Case> findByStatus(Case.CaseStatus status);
}
