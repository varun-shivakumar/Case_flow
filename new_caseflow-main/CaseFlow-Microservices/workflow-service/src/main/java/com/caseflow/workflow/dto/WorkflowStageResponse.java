package com.caseflow.workflow.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowStageResponse {
    private Long stageId; private Long caseId; private Integer sequenceNumber;
    private String roleResponsible; private Integer slaDays; private String stageName;
    private LocalDateTime startedAt; private LocalDateTime completedAt; private Boolean active;
    private Boolean skipped; private String skipReason; private String previousRole;
}
