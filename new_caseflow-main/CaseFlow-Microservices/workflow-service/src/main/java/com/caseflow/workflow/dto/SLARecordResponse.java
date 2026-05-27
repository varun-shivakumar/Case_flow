package com.caseflow.workflow.dto;
import com.caseflow.workflow.entity.SLARecord;
import lombok.Data;
import java.time.LocalDate;

@Data
public class SLARecordResponse {
    private Long slaRecordId; private Long caseId; private Long stageId;
    private LocalDate startDate; private LocalDate endDate;
    private SLARecord.SLAStatus status; private Integer slaDays;
    private Boolean breachNotified; private Long daysElapsed; private Long daysRemaining;
    private Boolean warningNotified; private Integer originalSlaDays;
    private String extensionReason;
    // calculated percentage — how much of SLA is consumed
    private Double slaUsagePercent;
}
