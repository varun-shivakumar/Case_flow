package com.caseflow.hearing.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Minimal projection of a Case from case-service — fields needed to fan out
 * hearing notifications to the litigant and the lawyer (if any).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseRef {
    private Long caseId;
    private String title;
    private String litigantId;
    private String lawyerId;
}
