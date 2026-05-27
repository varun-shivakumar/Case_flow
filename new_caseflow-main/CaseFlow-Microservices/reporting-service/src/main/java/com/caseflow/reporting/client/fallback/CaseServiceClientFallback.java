package com.caseflow.reporting.client.fallback;

import com.caseflow.reporting.client.CaseServiceClient;
import com.caseflow.reporting.client.dto.CaseDto;
import com.caseflow.reporting.client.dto.DocumentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class CaseServiceClientFallback implements CaseServiceClient {

    @Override
    public List<CaseDto> getAllCases() {
        log.warn("case-service unavailable — returning empty case list for report");
        return Collections.emptyList();
    }

    @Override
    public CaseDto getCaseById(Long caseId) {
        log.warn("case-service unavailable — returning null for case #{}", caseId);
        return null;
    }

    @Override
    public List<DocumentDto> getDocumentsByCase(Long caseId) {
        log.warn("case-service unavailable — returning empty document list for case #{}", caseId);
        return Collections.emptyList();
    }

    @Override
    public List<CaseDto> getCasesByLitigant(String litigantId) {
        log.warn("case-service unavailable — returning empty list for litigant {}", litigantId);
        return Collections.emptyList();
    }

    @Override
    public List<CaseDto> getCasesByLawyer(String lawyerId) {
        log.warn("case-service unavailable — returning empty list for lawyer {}", lawyerId);
        return Collections.emptyList();
    }
}
