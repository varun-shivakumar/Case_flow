package com.caseflow.appeals.client.fallback;

import com.caseflow.appeals.client.WorkflowServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WorkflowServiceFallback implements FallbackFactory<WorkflowServiceClient> {

    @Override
    public WorkflowServiceClient create(Throwable cause) {
        return caseId -> log.warn(
            "CIRCUIT BREAKER: workflow-service unavailable — advanceWorkflow({}) skipped | cause: {}",
            caseId,
            cause != null ? cause.getMessage() : "Unknown error");
    }
}
