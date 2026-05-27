package com.caseflow.appeals.client.fallback;

import com.caseflow.appeals.client.CaseServiceClient;
import com.caseflow.appeals.dto.response.CaseOwnerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@Slf4j
public class CaseServiceFallback implements FallbackFactory<CaseServiceClient> {

    @Override
    public CaseServiceClient create(Throwable cause) {
        return new CaseServiceClient() {

            @Override
            public CaseOwnerInfo getCaseDetails(Long caseId) {
                log.warn("CIRCUIT BREAKER [getCaseDetails]: case-service unavailable for case #{} — cause: {}",
                    caseId, cause != null ? cause.getMessage() : "Unknown error");
                return null;
            }

            @Override
            public Map<String, Object> updateCaseStatusInternal(Long caseId, String newStatus) {
                log.error("CIRCUIT BREAKER [updateCaseStatusInternal]: case-service unavailable — " +
                          "case #{} status NOT updated to [{}]. Cause: {}", caseId, newStatus,
                          cause != null ? cause.getMessage() : "Unknown error");
                return Map.of(
                    "status", "FALLBACK",
                    "caseId", caseId,
                    "error", "Case service is temporarily unavailable",
                    "requestedStatus", newStatus,
                    "cause", cause != null ? cause.getMessage() : "Unknown error"
                );
            }
        };
    }
}
