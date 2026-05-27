package com.caseflow.workflow.dto;
import lombok.Data;
@Data
public class ManualStageRequest {
    private Integer sequenceNumber;
    private String roleResponsible;
    private Integer slaDays;
    private String stageName;
}
