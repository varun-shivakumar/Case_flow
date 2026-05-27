package com.caseflow.appeals.dto.response;

import com.caseflow.appeals.entity.Appeal.AppealStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO returned for all appeal read/write operations.
 */
@Data
@Builder
public class AppealResponse {
    private Long          appealId;
    private Long          caseId;
    private String        filedByUserId;
    private LocalDateTime filedDate;
    private String        reason;
    private AppealStatus  status;
}
