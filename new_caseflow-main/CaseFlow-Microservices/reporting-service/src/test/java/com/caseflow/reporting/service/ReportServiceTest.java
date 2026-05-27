package com.caseflow.reporting.service;

import com.caseflow.reporting.client.AppealServiceClient;
import com.caseflow.reporting.client.CaseServiceClient;
import com.caseflow.reporting.client.ComplianceServiceClient;
import com.caseflow.reporting.client.HearingServiceClient;
import com.caseflow.reporting.client.WorkflowServiceClient;
import com.caseflow.reporting.exception.ResourceNotFoundException;
import com.caseflow.reporting.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private CaseServiceClient caseClient;
    @Mock private HearingServiceClient hearingClient;
    @Mock private WorkflowServiceClient workflowClient;
    @Mock private AppealServiceClient appealClient;
    @Mock private ComplianceServiceClient complianceClient;

    @InjectMocks private ReportServiceImpl reportService;

    @Test
    void getReportById_notFound_throws() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> reportService.getReportById(99L));
    }

    @Test
    void getReportsByUser_empty() {
        when(reportRepository.findByRequestedByOrderByGeneratedDateDesc("admin@example.com"))
                .thenReturn(List.of());
        assertEquals(0, reportService.getReportsByUser("admin@example.com").size());
    }
}
