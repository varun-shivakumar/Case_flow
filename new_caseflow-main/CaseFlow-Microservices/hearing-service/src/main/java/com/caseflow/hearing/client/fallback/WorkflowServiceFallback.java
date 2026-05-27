package com.caseflow.hearing.client.fallback;

import com.caseflow.hearing.client.WorkflowServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component @Slf4j
public class WorkflowServiceFallback implements WorkflowServiceClient {

    @Override
    public void advanceWorkflow(Long caseId) {
        log.warn("CIRCUIT BREAKER: workflow-service unavailable — advanceWorkflow({}) skipped", caseId);
    }
}
