package com.caseflow.compliance.client.fallback;

import com.caseflow.compliance.client.WorkflowServiceClient;
import com.caseflow.compliance.client.dto.SLARecordDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class WorkflowServiceClientFallback implements WorkflowServiceClient {

    @Override
    public List<SLARecordDto> getSlaRecordsByCase(Long caseId) {
        log.warn("workflow-service unavailable — skipping SLA check for case {}", caseId);
        return Collections.emptyList();
    }
}
