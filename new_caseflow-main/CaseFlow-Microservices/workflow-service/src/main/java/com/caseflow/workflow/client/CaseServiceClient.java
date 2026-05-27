package com.caseflow.workflow.client;

import com.caseflow.workflow.client.fallback.CaseServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "case-service", fallback = CaseServiceFallback.class)
public interface CaseServiceClient {
    @PatchMapping("/api/cases/internal/{caseId}/type")
    void setCaseType(@PathVariable("caseId") Long caseId, @RequestParam("caseType") String caseType);
}
