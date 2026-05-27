package com.caseflow.appeals.client;

import com.caseflow.appeals.client.fallback.WorkflowServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "workflow-service", fallbackFactory = WorkflowServiceFallback.class)
public interface WorkflowServiceClient {
    @PostMapping("/api/workflow/cases/{caseId}/advance")
    void advanceWorkflow(@PathVariable("caseId") Long caseId);
}
