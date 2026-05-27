package com.caseflow.appeals.dto.response;

import com.caseflow.appeals.entity.Appeal.AppealStatus;
import com.caseflow.appeals.entity.AppealAudit.Action;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppealAuditResponse {
    private Long          auditId;
    private Long          appealId;
    private Action        action;
    private String        actorUserId;
    private String        actorRole;
    private AppealStatus  fromStatus;
    private AppealStatus  toStatus;
    private String        metadata;
    private LocalDateTime timestamp;
}
