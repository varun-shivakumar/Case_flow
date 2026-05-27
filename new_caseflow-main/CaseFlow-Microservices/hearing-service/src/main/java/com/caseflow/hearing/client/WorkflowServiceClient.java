package com.caseflow.hearing.client;

import com.caseflow.hearing.client.fallback.WorkflowServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "workflow-service", fallback = WorkflowServiceFallback.class)
public interface WorkflowServiceClient {
    @PostMapping("/api/workflow/cases/{caseId}/advance")
    void advanceWorkflow(@PathVariable("caseId") Long caseId);
}
