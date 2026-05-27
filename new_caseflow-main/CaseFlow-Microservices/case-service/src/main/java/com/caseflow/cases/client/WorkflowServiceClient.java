package com.caseflow.cases.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "workflow-service")
public interface WorkflowServiceClient {
    @PostMapping("/api/workflow/cases/{caseId}/advance")
    void advanceWorkflow(@PathVariable("caseId") Long caseId);
}
