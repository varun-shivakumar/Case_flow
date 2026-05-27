package com.caseflow.appeals.dto.request;

import com.caseflow.appeals.entity.Review.ReviewOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for issuing a final decision on a REVIEWED appeal.
 * Transitions the appeal: REVIEWED → DECIDED.
 */
@Data
public class DecisionRequest {

    @NotNull(message = "outcome is required")
    private ReviewOutcome outcome;

    private String remarks;
}
