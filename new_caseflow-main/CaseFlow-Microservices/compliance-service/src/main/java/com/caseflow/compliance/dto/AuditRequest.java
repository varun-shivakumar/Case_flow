package com.caseflow.compliance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuditRequest {

    @NotBlank(message = "Scope is required")
    private String scope;

    private String findings;
}
