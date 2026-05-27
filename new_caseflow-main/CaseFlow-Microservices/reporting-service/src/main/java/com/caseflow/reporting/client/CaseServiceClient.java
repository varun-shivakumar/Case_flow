package com.caseflow.reporting.client;

import com.caseflow.reporting.client.dto.CaseDto;
import com.caseflow.reporting.client.dto.DocumentDto;
import com.caseflow.reporting.client.fallback.CaseServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "case-service", fallback = CaseServiceClientFallback.class)
public interface CaseServiceClient {

    @GetMapping("/api/cases")
    List<CaseDto> getAllCases();

    @GetMapping("/api/cases/{caseId}")
    CaseDto getCaseById(@PathVariable Long caseId);

    @GetMapping("/api/cases/{caseId}/documents")
    List<DocumentDto> getDocumentsByCase(@PathVariable Long caseId);

    @GetMapping("/api/cases/litigant/{litigantId}")
    List<CaseDto> getCasesByLitigant(@PathVariable String litigantId);

    @GetMapping("/api/cases/lawyer/{lawyerId}")
    List<CaseDto> getCasesByLawyer(@PathVariable String lawyerId);
}
