package com.caseflow.appeals.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Lean inter-service DTO — captures only the fields appeal-service needs from
 * the case-service CaseResponse: ownership, status, and closedDate (used to
 * enforce the appeal-filing deadline).
 * Jackson ignores all other fields (caseId, title, filedDate, etc.).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseOwnerInfo {
    private String        litigantId;
    private String        lawyerId;     // nullable — not every case has an assigned lawyer
    private String        status;       // FILED | ACTIVE | ADJOURNED | CLOSED
    private LocalDateTime closedDate;   // null unless status == CLOSED
}
