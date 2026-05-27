package com.caseflow.hearing.client;

import com.caseflow.hearing.client.dto.CaseRef;
import com.caseflow.hearing.client.fallback.CaseServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "case-service", fallback = CaseServiceFallback.class)
public interface CaseServiceClient {
    @PatchMapping("/api/cases/internal/{caseId}/status")
    void updateCaseStatusInternal(@PathVariable("caseId") Long caseId, @RequestParam("newStatus") String newStatus);

    /** Fetch case info (litigantId / lawyerId) so we can fan out hearing notifications. */
    @GetMapping("/api/cases/{caseId}")
    CaseRef getCaseById(@PathVariable("caseId") Long caseId);
}
