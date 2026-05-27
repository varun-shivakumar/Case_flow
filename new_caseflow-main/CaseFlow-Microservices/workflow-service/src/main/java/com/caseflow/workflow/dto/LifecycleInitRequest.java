package com.caseflow.workflow.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class LifecycleInitRequest {
    @NotBlank private String caseType = "civil";
    @NotBlank private String mode = "auto";
    private List<ManualStageRequest> stages;
}
