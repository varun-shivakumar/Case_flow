package com.caseflow.appeals.repository;

import com.caseflow.appeals.entity.AppealAudit;
import com.caseflow.appeals.entity.AppealAudit.Action;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealAuditRepository extends JpaRepository<AppealAudit, Long> {

    /** Newest first — caller almost always wants reverse-chronological. */
    List<AppealAudit> findByAppealIdOrderByTimestampDesc(Long appealId);

    Page<AppealAudit> findByActorUserIdOrderByTimestampDesc(String actorUserId, Pageable pageable);

    Page<AppealAudit> findByActionOrderByTimestampDesc(Action action, Pageable pageable);
}
