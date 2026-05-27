package com.caseflow.compliance.client;

import com.caseflow.compliance.client.dto.CaseDto;
import com.caseflow.compliance.client.dto.DocumentDto;
import com.caseflow.compliance.client.fallback.CaseServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "case-service", fallback = CaseServiceClientFallback.class)
public interface CaseServiceClient {

    @GetMapping("/api/cases")
    List<CaseDto> getAllCases();

    @GetMapping("/api/cases/{caseId}/documents")
    List<DocumentDto> getDocumentsByCase(@PathVariable Long caseId);
}
