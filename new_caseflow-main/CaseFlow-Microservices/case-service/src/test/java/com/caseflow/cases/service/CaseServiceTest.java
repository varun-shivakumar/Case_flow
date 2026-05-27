package com.caseflow.cases.service;

import com.caseflow.cases.client.*;
import com.caseflow.cases.dto.*;
import com.caseflow.cases.entity.Case;
import com.caseflow.cases.exception.*;
import com.caseflow.cases.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {
    @Mock private CaseRepository caseRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private IamServiceClient iamClient;
    @Mock private WorkflowServiceClient workflowClient;
    @Mock private NotificationServiceClient notificationClient;
    @InjectMocks private CaseService caseService;

    @Test void getCaseById_found() {
        Case c = Case.builder().caseId(1L).title("Test Case").litigantId("user-1")
            .filedDate(LocalDateTime.now()).status(Case.CaseStatus.FILED).build();
        when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
        CaseResponse result = caseService.getCaseById(1L);
        assertEquals("Test Case", result.getTitle());
    }

    @Test void getCaseById_notFound_throws() {
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> caseService.getCaseById(99L));
    }

    @Test void fileCase_litigantNotFound_throws() {
        when(iamClient.existsById("99")).thenReturn(false);
        CaseRequest req = new CaseRequest();
        req.setTitle("Test"); req.setLitigantId("99");
        assertThrows(ResourceNotFoundException.class, () -> caseService.fileCase(req));
    }

    @Test void getAllCases_empty() {
        when(caseRepository.findAll()).thenReturn(List.of());
        assertEquals(0, caseService.getAllCases().size());
    }
}
