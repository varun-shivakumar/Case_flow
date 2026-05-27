package com.caseflow.appeals.client;

import com.caseflow.appeals.client.fallback.CaseServiceFallback;
import com.caseflow.appeals.dto.response.CaseOwnerInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Feign client for inter-service communication with case-service.
 *
 * Feign deserializes the JSON body of HTTP responses; case-service returns
 * ResponseEntity<CaseResponse>, so the body is deserialized into CaseOwnerInfo
 * (extra fields are ignored via @JsonIgnoreProperties).
 *
 * Fallback is triggered on network errors, timeouts, or circuit breaker open state.
 */
@FeignClient(name = "case-service", fallbackFactory = CaseServiceFallback.class)
public interface CaseServiceClient {

    /**
     * Fetches case ownership info (litigantId, lawyerId, status) in a single call.
     * Returns null from fallback when case-service is unreachable.
     */
    @GetMapping("/api/cases/{caseId}")
    CaseOwnerInfo getCaseDetails(@PathVariable("caseId") Long caseId);

    /**
     * Updates case status via internal endpoint.
     * Returned Map carries the updated case body, or a {"status":"FALLBACK", ...}
     * marker if case-service is unavailable.
     */
    @PatchMapping("/api/cases/internal/{caseId}/status")
    Map<String, Object> updateCaseStatusInternal(
        @PathVariable("caseId") Long caseId,
        @RequestParam("newStatus") String newStatus
    );
}
