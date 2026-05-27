package com.caseflow.appeals.service;

import com.caseflow.appeals.dto.response.AppealAuditResponse;
import com.caseflow.appeals.entity.Appeal.AppealStatus;
import com.caseflow.appeals.entity.AppealAudit;
import com.caseflow.appeals.entity.AppealAudit.Action;
import com.caseflow.appeals.repository.AppealAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Records and queries appeal audit entries.
 *
 * Audit writes use Propagation.MANDATORY — every record() call must run inside
 * the calling service's transaction so the audit row commits atomically with
 * the state change it describes (or rolls back together).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppealAuditService {

    private final AppealAuditRepository auditRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long appealId, Action action,
                       String actorUserId, String actorRole,
                       AppealStatus fromStatus, AppealStatus toStatus,
                       String metadata) {
        AppealAudit entry = AppealAudit.builder()
            .appealId(appealId)
            .action(action)
            .actorUserId(actorUserId == null || actorUserId.isBlank() ? "system" : actorUserId)
            .actorRole(actorRole)
            .fromStatus(fromStatus)
            .toStatus(toStatus)
            .metadata(metadata)
            .timestamp(LocalDateTime.now())
            .build();
        auditRepository.save(entry);
        log.debug("Audit: appeal #{} {} by [{}] ({}→{})",
            appealId, action, actorUserId, fromStatus, toStatus);
    }

    @Transactional(readOnly = true)
    public List<AppealAuditResponse> getAuditTrail(Long appealId) {
        return auditRepository.findByAppealIdOrderByTimestampDesc(appealId)
            .stream().map(AppealAuditService::toResponse).toList();
    }

    static AppealAuditResponse toResponse(AppealAudit a) {
        return AppealAuditResponse.builder()
            .auditId(a.getAuditId())
            .appealId(a.getAppealId())
            .action(a.getAction())
            .actorUserId(a.getActorUserId())
            .actorRole(a.getActorRole())
            .fromStatus(a.getFromStatus())
            .toStatus(a.getToStatus())
            .metadata(a.getMetadata())
            .timestamp(a.getTimestamp())
            .build();
    }
}
