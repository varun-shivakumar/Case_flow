package com.caseflow.hearing.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class HearingRequest {
    @NotNull private Long caseId;
    @NotBlank private String judgeId;
    @NotNull @FutureOrPresent private LocalDate hearingDate;
    @NotBlank private String hearingTime;
    @NotBlank private String scheduledBy;
}
