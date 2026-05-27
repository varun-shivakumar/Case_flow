package com.caseflow.cases.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CaseRequest {
    @NotBlank @Size(min = 3, max = 255) private String title;
    @NotNull private String litigantId;
    private String lawyerId;
}
