package com.caseflow.hearing.service;

import com.caseflow.hearing.client.*;
import com.caseflow.hearing.entity.*;
import com.caseflow.hearing.exception.*;
import com.caseflow.hearing.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HearingServiceTest {
    @Mock private HearingRepository hearingRepository;
    @Mock private CaseServiceClient caseClient;
    @Mock private IamServiceClient iamClient;
    @Mock private WorkflowServiceClient workflowClient;
    @Mock private NotificationServiceClient notificationClient;
    @InjectMocks private HearingService hearingService;

    @Test void getHearingById_notFound_throws() {
        when(hearingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> hearingService.getHearingById(99L));
    }

    @Test void getHearingById_found() {
        Hearing h = Hearing.builder().hearingId(1L).caseId(1L).judgeId("user-1")
            .hearingDate(LocalDate.now()).hearingTime("10:00 AM - 11:00 AM")
            .status(Hearing.HearingStatus.SCHEDULED).scheduledBy("user-1").build();
        when(hearingRepository.findById(1L)).thenReturn(Optional.of(h));
        assertEquals(1L, hearingService.getHearingById(1L).getHearingId());
    }

    @Test void getAllHearings_empty() {
        when(hearingRepository.findAll()).thenReturn(List.of());
        assertEquals(0, hearingService.getAllHearings().size());
    }
}
