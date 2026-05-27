package com.caseflow.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDto {
    private Long caseId;
    private String title;
    private String status;          // FILED, ACTIVE, ADJOURNED, DECIDED, CLOSED, etc.
    private String caseType;        // civil, criminal, corporate
    private String litigantId;
    private String lawyerId;
    /** Judge user-id (IAM format, e.g. "JOH_JUDGE_1"). */
    private String judgeId;
    private LocalDate filedDate;
}
