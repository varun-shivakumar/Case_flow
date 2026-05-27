package com.caseflow.workflow.client.fallback;

import com.caseflow.workflow.client.CaseServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component @Slf4j
public class CaseServiceFallback implements CaseServiceClient {

    @Override
    public void setCaseType(Long caseId, String caseType) {
        log.warn("CIRCUIT BREAKER: case-service unavailable — setCaseType({}, {}) skipped", caseId, caseType);
    }
}
