package com.caseflow.compliance.dto;
import com.caseflow.compliance.entity.Audit.AuditStatus;
import lombok.*;
import java.time.LocalDate;
@Data @Builder
public class AuditResponse {
    private Long auditId; private String adminId; private String scope;
    private String findings; private LocalDate date; private AuditStatus status;
}
