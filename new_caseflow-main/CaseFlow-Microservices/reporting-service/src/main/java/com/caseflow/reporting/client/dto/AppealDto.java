package com.caseflow.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppealDto {
    private Long appealId;
    private Long caseId;
    private String filedByUserId;
    private String status;         // FILED, UNDER_REVIEW, DECISION_ISSUED, CLOSED
    private LocalDate filedDate;
    private LocalDate decisionDate;
}
