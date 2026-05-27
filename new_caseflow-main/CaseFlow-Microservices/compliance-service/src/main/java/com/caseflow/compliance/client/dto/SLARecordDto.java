package com.caseflow.compliance.client.dto;

import lombok.Data;

@Data
public class SLARecordDto {
    private Long slaId;
    private String stageName;
    private String status;
}
