package com.caseflow.compliance.client;

import com.caseflow.compliance.client.dto.SLARecordDto;
import com.caseflow.compliance.client.fallback.WorkflowServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "workflow-service", fallback = WorkflowServiceClientFallback.class)
public interface WorkflowServiceClient {

    @GetMapping("/api/workflow/cases/{caseId}/sla")
    List<SLARecordDto> getSlaRecordsByCase(@PathVariable Long caseId);
}
