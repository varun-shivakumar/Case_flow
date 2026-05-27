package com.caseflow.appeals.service;

import com.caseflow.appeals.client.CaseServiceClient;
import com.caseflow.appeals.entity.Appeal;
import com.caseflow.appeals.exception.ResourceNotFoundException;
import com.caseflow.appeals.repository.AppealRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppealServiceTest {

    @Mock private AppealRepository           appealRepository;
    @Mock private CaseServiceClient          caseClient;
    @Mock private ApplicationEventPublisher  events;
    @Mock private AppealAuditService         audit;

    @InjectMocks private AppealService appealService;

    @Test
    void getAppealById_notFound_throws() {
        when(appealRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> appealService.getAppealById(99L));
    }

    @Test
    void getAppealById_found() {
        Appeal a = Appeal.builder()
            .appealId(1L)
            .caseId(1L)
            .filedByUserId("user-1")
            .filedDate(LocalDateTime.now())
            .reason("test")
            .status(Appeal.AppealStatus.SUBMITTED)
            .build();
        when(appealRepository.findById(1L)).thenReturn(Optional.of(a));

        assertEquals(1L, appealService.getAppealById(1L).getAppealId());
    }

    @Test
    void getAppealsByCase_empty() {
        when(appealRepository.findByCaseId(1L)).thenReturn(List.of());
        assertEquals(0, appealService.getAppealsByCase(1L).size());
    }
}
