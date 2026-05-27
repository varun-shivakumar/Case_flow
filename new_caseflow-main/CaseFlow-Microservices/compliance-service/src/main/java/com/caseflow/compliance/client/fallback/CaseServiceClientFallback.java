package com.caseflow.compliance.client.fallback;

import com.caseflow.compliance.client.CaseServiceClient;
import com.caseflow.compliance.client.dto.CaseDto;
import com.caseflow.compliance.client.dto.DocumentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class CaseServiceClientFallback implements CaseServiceClient {

    @Override
    public List<CaseDto> getAllCases() {
        log.warn("case-service unavailable — returning empty case list");
        return Collections.emptyList();
    }

    @Override
    public List<DocumentDto> getDocumentsByCase(Long caseId) {
        log.warn("case-service unavailable — skipping document check for case {}", caseId);
        return Collections.emptyList();
    }
}
