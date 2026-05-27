package com.caseflow.appeals.dto.request;

import com.caseflow.appeals.entity.Review.ReviewOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for updating the draft outcome of a REVIEWED appeal.
 * Does NOT transition the appeal to DECIDED — use DecisionRequest for that.
 */
@Data
public class UpdateOutcomeRequest {

    @NotNull(message = "outcome is required")
    private ReviewOutcome outcome;
}
