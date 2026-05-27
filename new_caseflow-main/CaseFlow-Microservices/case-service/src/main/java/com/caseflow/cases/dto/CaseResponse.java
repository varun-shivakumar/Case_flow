package com.caseflow.cases.dto;
import com.caseflow.cases.entity.Case;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CaseResponse {
    private Long caseId;
    private String title;
    private String litigantId;
    private String lawyerId;
    private LocalDateTime filedDate;
    private LocalDateTime closedDate;
    private Case.CaseStatus status;
}
