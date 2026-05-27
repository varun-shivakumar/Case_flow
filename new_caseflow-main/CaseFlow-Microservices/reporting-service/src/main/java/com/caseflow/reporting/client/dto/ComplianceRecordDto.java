package com.caseflow.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComplianceRecordDto {
    private Long complianceId;
    private Long caseId;
    private String type;         // DOCUMENT, PROCESS
    private String result;       // PASS, FAIL
    private LocalDate date;
    private String notes;
}
