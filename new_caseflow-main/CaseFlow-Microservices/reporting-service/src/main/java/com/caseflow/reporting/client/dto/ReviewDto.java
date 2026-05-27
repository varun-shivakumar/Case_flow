package com.caseflow.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewDto {
    private Long reviewId;
    private Long appealId;
    private Long caseId;
    /** Judge user-id (IAM format, e.g. "JOH_JUDGE_1"). */
    private String judgeId;
    /** User-id of the clerk who opened this review and assigned the judge. */
    private String assignedByClerkId;
    private String outcome;       // UPHELD, REVERSED, MODIFIED, SENT_BACK
    private LocalDate reviewDate;
}
