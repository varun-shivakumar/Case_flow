package com.caseflow.workflow.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReassignRoleRequest {
    @NotNull(message = "Stage ID is required")
    private Long stageId;

    @NotBlank(message = "New role is required")
    private String newRole;
}
