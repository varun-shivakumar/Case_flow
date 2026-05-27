package com.caseflow.reporting.client;

import com.caseflow.reporting.client.dto.ComplianceRecordDto;
import com.caseflow.reporting.client.dto.PageResponse;
import com.caseflow.reporting.client.fallback.ComplianceServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "compliance-service", fallback = ComplianceServiceClientFallback.class)
public interface ComplianceServiceClient {

    @GetMapping("/api/compliance/paginated")
    PageResponse<ComplianceRecordDto> getCompliancePaginated(
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/api/compliance/case/{caseId}")
    List<ComplianceRecordDto> getComplianceByCase(@PathVariable Long caseId);
}
