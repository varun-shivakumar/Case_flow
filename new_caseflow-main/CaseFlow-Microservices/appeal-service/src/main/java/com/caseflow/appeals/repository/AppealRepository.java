package com.caseflow.appeals.repository;

import com.caseflow.appeals.entity.Appeal;
import com.caseflow.appeals.entity.Appeal.AppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealRepository extends JpaRepository<Appeal, Long> {
    List<Appeal> findByCaseId(Long caseId);
    List<Appeal> findByFiledByUserId(String userId);
    List<Appeal> findByStatus(AppealStatus status);

    Page<Appeal> findByCaseId(Long caseId, Pageable pageable);
    Page<Appeal> findByFiledByUserId(String userId, Pageable pageable);
    Page<Appeal> findByStatus(AppealStatus status, Pageable pageable);

    long countByCaseId(Long caseId);
    boolean existsByCaseIdAndStatus(Long caseId, AppealStatus status);
}
