package com.caseflow.cases.dto;
import com.caseflow.cases.entity.Document;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerificationRequest {
    @NotNull private Document.VerificationStatus status;
    private String rejectionReason;
    @NotNull private String clerkId;
}
