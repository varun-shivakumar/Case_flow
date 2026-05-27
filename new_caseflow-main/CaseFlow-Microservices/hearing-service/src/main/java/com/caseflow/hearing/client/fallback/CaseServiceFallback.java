package com.caseflow.hearing.client.fallback;

import com.caseflow.hearing.client.CaseServiceClient;
import com.caseflow.hearing.client.dto.CaseRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component @Slf4j
public class CaseServiceFallback implements CaseServiceClient {

    @Override
    public void updateCaseStatusInternal(Long caseId, String newStatus) {
        log.warn("CIRCUIT BREAKER: case-service unavailable — updateCaseStatus({}, {}) skipped", caseId, newStatus);
    }

    @Override
    public CaseRef getCaseById(Long caseId) {
        log.warn("CIRCUIT BREAKER: case-service unavailable — getCaseById({}) returning null", caseId);
        return null;
    }
}
