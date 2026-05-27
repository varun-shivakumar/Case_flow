package com.caseflow.hearing.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CompleteHearingRequest {
    @NotNull private String judgeId;
    @NotBlank @Size(min = 10, max = 2000) private String hearingNotes;
}
