package com.caseflow.reporting.client;

import com.caseflow.reporting.client.dto.SLARecordDto;
import com.caseflow.reporting.client.fallback.WorkflowServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "workflow-service", fallback = WorkflowServiceClientFallback.class)
public interface WorkflowServiceClient {

    @GetMapping("/api/workflow/cases/{caseId}/sla")
    List<SLARecordDto> getSlaRecordsByCase(@PathVariable Long caseId);

    @GetMapping("/api/workflow/sla/breached")
    List<SLARecordDto> getBreachedSlas();

    @GetMapping("/api/workflow/sla/active")
    List<SLARecordDto> getActiveSlas();
}
