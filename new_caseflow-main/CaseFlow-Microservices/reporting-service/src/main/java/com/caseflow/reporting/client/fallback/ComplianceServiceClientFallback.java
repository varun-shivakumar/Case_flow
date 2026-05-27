package com.caseflow.reporting.client.fallback;

import com.caseflow.reporting.client.ComplianceServiceClient;
import com.caseflow.reporting.client.dto.ComplianceRecordDto;
import com.caseflow.reporting.client.dto.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ComplianceServiceClientFallback implements ComplianceServiceClient {

    @Override
    public PageResponse<ComplianceRecordDto> getCompliancePaginated(int page, int size) {
        log.warn("compliance-service unavailable — returning empty paginated compliance records");
        PageResponse<ComplianceRecordDto> empty = new PageResponse<>();
        empty.setContent(Collections.emptyList());
        empty.setEmpty(true);
        empty.setTotalElements(0);
        empty.setTotalPages(0);
        return empty;
    }

    @Override
    public List<ComplianceRecordDto> getComplianceByCase(Long caseId) {
        log.warn("compliance-service unavailable — returning empty list for case #{}", caseId);
        return Collections.emptyList();
    }
}
