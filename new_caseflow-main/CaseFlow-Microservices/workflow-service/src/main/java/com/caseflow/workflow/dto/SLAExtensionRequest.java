package com.caseflow.workflow.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SLAExtensionRequest {
    @NotNull(message = "Additional days is required")
    @Min(value = 1, message = "Must extend by at least 1 day")
    private Integer additionalDays;

    @NotBlank(message = "Extension reason is required")
    @Size(min = 5, max = 1000, message = "Reason must be between 5 and 1000 characters")
    private String reason;
}
