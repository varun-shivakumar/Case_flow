package com.caseflow.compliance.dto;

import lombok.Data;

import java.util.List;

@Data
public class ComplianceCheckRequest {
    private List<Long> caseIds;
    private String dateFrom;
    private String dateTo;
}
