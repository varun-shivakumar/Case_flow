package com.caseflow.appeals.dto.response;

import com.caseflow.appeals.entity.Review.ReviewOutcome;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO returned for all review read/write operations.
 */
@Data
@Builder
public class ReviewResponse {
    private Long          reviewId;
    private Long          caseId;
    private Long          appealId;
    private String        judgeId;
    /** User-id of the clerk/admin who opened this review and assigned the judge. */
    private String        assignedByClerkId;
    private LocalDateTime reviewDate;
    private ReviewOutcome outcome;
    private String        remarks;
}
