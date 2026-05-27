package com.caseflow.reporting.service;

import com.caseflow.reporting.dto.ReportResponse;
import com.caseflow.reporting.entity.Report;
import com.caseflow.reporting.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedReportService {

    private final ReportRepository reportRepository;

    public Page<ReportResponse> getAllReportsPaginated(Pageable pageable) {
        return reportRepository.findAll(pageable).map(this::toResponse);
    }

    @Cacheable(value = "reports", key = "#reportId")
    public Report getCachedReportById(Long reportId) {
        return reportRepository.findById(reportId).orElse(null);
    }

    private ReportResponse toResponse(Report r) {
        return ReportResponse.builder()
                .reportId(r.getReportId())
                .scope(r.getScope())
                .scopeValue(r.getScopeValue())
                .metrics(r.getMetrics())
                .generatedDate(r.getGeneratedDate())
                .requestedBy(r.getRequestedBy())
                .dateFrom(r.getDateFrom())
                .dateTo(r.getDateTo())
                .build();
    }
}
