package com.caseflow.reporting.client.fallback;

import com.caseflow.reporting.client.WorkflowServiceClient;
import com.caseflow.reporting.client.dto.SLARecordDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class WorkflowServiceClientFallback implements WorkflowServiceClient {

    @Override
    public List<SLARecordDto> getSlaRecordsByCase(Long caseId) {
        log.warn("workflow-service unavailable — returning empty SLA list for case #{}", caseId);
        return Collections.emptyList();
    }

    @Override
    public List<SLARecordDto> getBreachedSlas() {
        log.warn("workflow-service unavailable — returning empty breached SLA list");
        return Collections.emptyList();
    }

    @Override
    public List<SLARecordDto> getActiveSlas() {
        log.warn("workflow-service unavailable — returning empty active SLA list");
        return Collections.emptyList();
    }
}
