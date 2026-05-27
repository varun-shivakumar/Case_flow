package com.caseflow.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingDto {
    private Long hearingId;
    private Long caseId;
    /** Judge user-id (IAM format, e.g. "JOH_JUDGE_1"). */
    private String judgeId;
    private String status;          // SCHEDULED, RESCHEDULED, COMPLETED, CANCELLED
    private LocalDate hearingDate;
    /** User-id of the clerk who scheduled this hearing. */
    private String scheduledBy;
}
