package com.caseflow.appeals.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for filing a new appeal.
 * filedByUserId is NOT accepted here — it is auto-populated from the JWT
 * via the X-Auth-User-Id header injected by the API Gateway.
 */
@Data
public class AppealRequest {

    @NotNull(message = "caseId is required")
    private Long caseId;

    @NotBlank(message = "Reason for appeal is required")
    private String reason;
}
