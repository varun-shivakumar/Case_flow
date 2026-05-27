package com.caseflow.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SLARecordDto {
    private Long slaId;
    private Long caseId;
    private String stageName;
    private String status;       // ACTIVE, WARNING, BREACHED, CLOSED
}
