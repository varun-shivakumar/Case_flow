package com.caseflow.hearing.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RescheduleRequest {
    @NotNull @FutureOrPresent private LocalDate newDate;
    @NotBlank private String newTime;
    @NotBlank @Size(min = 5, max = 500) private String rescheduleReason;
    @NotBlank private String clerkId;
}
