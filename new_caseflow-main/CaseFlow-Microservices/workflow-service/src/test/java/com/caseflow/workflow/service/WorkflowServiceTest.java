package com.caseflow.workflow.service;

import com.caseflow.workflow.client.*;
import com.caseflow.workflow.dto.*;
import com.caseflow.workflow.entity.*;
import com.caseflow.workflow.exception.*;
import com.caseflow.workflow.repository.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {
    @Mock private WorkflowStageRepository workflowStageRepository;
    @Mock private SLARecordRepository slaRecordRepository;
    @Mock private CaseServiceClient caseClient;
    @Mock private NotificationServiceClient notificationClient;
    @InjectMocks private WorkflowService workflowService;

    // ===================== EXISTING TESTS =====================

    @Nested class GetStages {
        @Test void getStagesByCaseId_returnsList() {
            WorkflowStage stage = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();
            when(workflowStageRepository.findByCaseIdOrderBySequenceNumber(1L)).thenReturn(List.of(stage));
            List<WorkflowStageResponse> result = workflowService.getStagesByCaseId(1L);
            assertEquals(1, result.size());
            assertEquals("Intake", result.get(0).getStageName());
        }

        @Test void getCurrentStage_notFound_throws() {
            when(workflowStageRepository.findByCaseIdAndActiveTrue(99L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> workflowService.getCurrentStage(99L));
        }

        @Test void getCurrentStage_found() {
            WorkflowStage stage = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();
            when(workflowStageRepository.findByCaseIdAndActiveTrue(1L)).thenReturn(Optional.of(stage));
            WorkflowStageResponse result = workflowService.getCurrentStage(1L);
            assertTrue(result.getActive());
            assertEquals("Intake", result.getStageName());
        }
    }

    @Nested class SLAQueries {
        @Test void getAllBreachedSLAs_empty() {
            when(slaRecordRepository.findByStatus(SLARecord.SLAStatus.BREACHED)).thenReturn(List.of());
            assertEquals(0, workflowService.getAllBreachedSLAs().size());
        }

        @Test void getAllWarningSLAs_empty() {
            when(slaRecordRepository.findByStatus(SLARecord.SLAStatus.WARNING)).thenReturn(List.of());
            assertEquals(0, workflowService.getAllWarningSLAs().size());
        }

        @Test void getAllActiveSLAs_filtersCorrectly() {
            SLARecord active = SLARecord.builder().slaRecordId(1L).caseId(1L).stageId(1L)
                .startDate(LocalDate.now()).slaDays(7).status(SLARecord.SLAStatus.ON_TIME)
                .breachNotified(false).warningNotified(false).build();
            SLARecord closed = SLARecord.builder().slaRecordId(2L).caseId(1L).stageId(2L)
                .startDate(LocalDate.now().minusDays(5)).endDate(LocalDate.now()).slaDays(5)
                .status(SLARecord.SLAStatus.COMPLETED).breachNotified(false).warningNotified(false).build();
            when(slaRecordRepository.findAll()).thenReturn(List.of(active, closed));
            assertEquals(1, workflowService.getAllActiveSLAs().size());
        }
    }

    // ===================== FEATURE 1: SLA EARLY WARNING =====================

    @Nested class SLAWarningCheck {
        @Test void slaCheck_triggersWarningAt80Percent() {
            // 10 day SLA, started 8 days ago → 80% consumed
            SLARecord sla = SLARecord.builder().slaRecordId(1L).caseId(1L).stageId(1L)
                .startDate(LocalDate.now().minusDays(8)).slaDays(10)
                .status(SLARecord.SLAStatus.ON_TIME).breachNotified(false)
                .warningNotified(false).build();
            when(slaRecordRepository.findAll()).thenReturn(List.of(sla));

            String result = workflowService.runManualSLACheck();

            assertTrue(result.contains("Warnings: 1"));
            assertEquals(SLARecord.SLAStatus.WARNING, sla.getStatus());
            assertTrue(sla.getWarningNotified());
            verify(notificationClient, atLeastOnce()).sendNotification(any());
        }

        @Test void slaCheck_triggersBreach() {
            // 5 day SLA, started 7 days ago → breached
            SLARecord sla = SLARecord.builder().slaRecordId(1L).caseId(1L).stageId(1L)
                .startDate(LocalDate.now().minusDays(7)).slaDays(5)
                .status(SLARecord.SLAStatus.ON_TIME).breachNotified(false)
                .warningNotified(false).build();
            when(slaRecordRepository.findAll()).thenReturn(List.of(sla));

            String result = workflowService.runManualSLACheck();

            assertTrue(result.contains("Breaches: 1"));
            assertEquals(SLARecord.SLAStatus.BREACHED, sla.getStatus());
        }

        @Test void slaCheck_doesNotDoubleWarn() {
            SLARecord sla = SLARecord.builder().slaRecordId(1L).caseId(1L).stageId(1L)
                .startDate(LocalDate.now().minusDays(9)).slaDays(10)
                .status(SLARecord.SLAStatus.WARNING).breachNotified(false)
                .warningNotified(true).build(); // already warned
            when(slaRecordRepository.findAll()).thenReturn(List.of(sla));

            String result = workflowService.runManualSLACheck();
            assertTrue(result.contains("Warnings: 0"));
        }
    }

    // ===================== FEATURE 2: ROLLBACK =====================

    @Nested class Rollback {
        @Test void rollback_atFirstStage_throws() {
            WorkflowStage stage1 = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();
            when(workflowStageRepository.findByCaseIdAndActiveTrue(1L)).thenReturn(Optional.of(stage1));

            assertThrows(InvalidOperationException.class, () -> workflowService.rollbackWorkflow(1L));
        }

        @Test void rollback_success_marksSlaAsRolledBack() {
            WorkflowStage stage2 = WorkflowStage.builder().stageId(2L).caseId(1L).sequenceNumber(2)
                .roleResponsible("JUDGE").slaDays(14).stageName("Trial")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();
            WorkflowStage stage1 = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now().minusDays(3)).completedAt(LocalDateTime.now())
                .active(false).skipped(false).build();
            SLARecord stage2Sla = SLARecord.builder().slaRecordId(2L).caseId(1L).stageId(2L)
                .startDate(LocalDate.now()).slaDays(14).status(SLARecord.SLAStatus.ON_TIME)
                .breachNotified(false).warningNotified(false).build();

            when(workflowStageRepository.findByCaseIdAndActiveTrue(1L)).thenReturn(Optional.of(stage2));
            when(slaRecordRepository.findByStageId(2L)).thenReturn(Optional.of(stage2Sla));
            when(workflowStageRepository.findByCaseIdAndSequenceNumber(1L, 1)).thenReturn(Optional.of(stage1));
            when(slaRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            WorkflowStageResponse result = workflowService.rollbackWorkflow(1L);

            assertEquals(1, result.getSequenceNumber());
            assertTrue(result.getActive());
            assertFalse(stage2.getActive());
            // the old stage's SLA should be marked ROLLED_BACK, not COMPLETED
            assertEquals(SLARecord.SLAStatus.ROLLED_BACK, stage2Sla.getStatus());
            assertNotNull(stage2Sla.getEndDate());
        }

        @Test void rollback_noActiveStage_throws() {
            when(workflowStageRepository.findByCaseIdAndActiveTrue(99L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> workflowService.rollbackWorkflow(99L));
        }
    }

    // ===================== FEATURE 3: SKIP STAGE =====================

    @Nested class SkipStage {
        @Test void skip_marksAsSkipped_andAdvances() {
            WorkflowStage current = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();
            WorkflowStage next = WorkflowStage.builder().stageId(2L).caseId(1L).sequenceNumber(2)
                .roleResponsible("JUDGE").slaDays(14).stageName("Trial")
                .startedAt(LocalDateTime.now()).active(false).skipped(false).build();

            when(workflowStageRepository.findByCaseIdAndActiveTrue(1L)).thenReturn(Optional.of(current));
            when(slaRecordRepository.findByStageId(1L)).thenReturn(Optional.of(
                SLARecord.builder().slaRecordId(1L).caseId(1L).stageId(1L)
                    .startDate(LocalDate.now()).slaDays(7).status(SLARecord.SLAStatus.ON_TIME)
                    .breachNotified(false).warningNotified(false).build()));
            when(workflowStageRepository.findByCaseIdAndSequenceNumber(1L, 2)).thenReturn(Optional.of(next));
            when(slaRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            WorkflowStageResponse result = workflowService.skipCurrentStage(1L, "Documents not required");

            assertTrue(current.getSkipped());
            assertEquals("Documents not required", current.getSkipReason());
            assertEquals(2, result.getSequenceNumber());
        }

        @Test void skip_lastStage_completesWorkflow() {
            WorkflowStage last = WorkflowStage.builder().stageId(4L).caseId(1L).sequenceNumber(4)
                .roleResponsible("JUDGE").slaDays(14).stageName("Judgment")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();

            when(workflowStageRepository.findByCaseIdAndActiveTrue(1L)).thenReturn(Optional.of(last));
            when(slaRecordRepository.findByStageId(4L)).thenReturn(Optional.empty());
            when(workflowStageRepository.findByCaseIdAndSequenceNumber(1L, 5)).thenReturn(Optional.empty());

            WorkflowStageResponse result = workflowService.skipCurrentStage(1L, "Settlement reached");

            assertTrue(last.getSkipped());
            assertEquals("Settlement reached", last.getSkipReason());
        }
    }

    // ===================== FEATURE 4: SLA EXTENSION =====================

    @Nested class SLAExtension {
        @Test void extend_success() {
            WorkflowStage current = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();
            SLARecord sla = SLARecord.builder().slaRecordId(1L).caseId(1L).stageId(1L)
                .startDate(LocalDate.now().minusDays(5)).slaDays(7)
                .status(SLARecord.SLAStatus.ON_TIME).breachNotified(false)
                .warningNotified(false).build();

            when(workflowStageRepository.findByCaseIdAndActiveTrue(1L)).thenReturn(Optional.of(current));
            when(slaRecordRepository.findByStageId(1L)).thenReturn(Optional.of(sla));
            when(slaRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SLAExtensionRequest req = new SLAExtensionRequest();
            req.setAdditionalDays(5); req.setReason("Awaiting external documents");

            SLARecordResponse result = workflowService.extendSLA(1L, req);

            assertEquals(12, result.getSlaDays()); // 7 + 5
            assertEquals(7, result.getOriginalSlaDays());
            assertEquals("Awaiting external documents", result.getExtensionReason());
            // workflow_stages.slaDays should NOT be changed — only sla_records
            assertEquals(7, current.getSlaDays());
            // workflowStageRepository.save should NOT be called for extension
            verify(workflowStageRepository, never()).save(any());
        }

        @Test void extend_breachedSLA_resetsStatus() {
            WorkflowStage current = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(5).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();
            SLARecord sla = SLARecord.builder().slaRecordId(1L).caseId(1L).stageId(1L)
                .startDate(LocalDate.now().minusDays(6)).slaDays(5)
                .status(SLARecord.SLAStatus.BREACHED).breachNotified(true)
                .warningNotified(true).build();

            when(workflowStageRepository.findByCaseIdAndActiveTrue(1L)).thenReturn(Optional.of(current));
            when(slaRecordRepository.findByStageId(1L)).thenReturn(Optional.of(sla));
            when(slaRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SLAExtensionRequest req = new SLAExtensionRequest();
            req.setAdditionalDays(10); req.setReason("Court delay");

            SLARecordResponse result = workflowService.extendSLA(1L, req);

            assertEquals(15, result.getSlaDays()); // 5 + 10
            // 6 elapsed out of 15 = 40%, should be ON_TIME
            assertNotEquals(SLARecord.SLAStatus.BREACHED, sla.getStatus());
        }

        @Test void extend_closedSLA_throws() {
            WorkflowStage current = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();
            SLARecord sla = SLARecord.builder().slaRecordId(1L).caseId(1L).stageId(1L)
                .startDate(LocalDate.now().minusDays(5)).endDate(LocalDate.now()).slaDays(7)
                .status(SLARecord.SLAStatus.COMPLETED).breachNotified(false)
                .warningNotified(false).build();

            when(workflowStageRepository.findByCaseIdAndActiveTrue(1L)).thenReturn(Optional.of(current));
            when(slaRecordRepository.findByStageId(1L)).thenReturn(Optional.of(sla));

            SLAExtensionRequest req = new SLAExtensionRequest();
            req.setAdditionalDays(3); req.setReason("test");

            assertThrows(InvalidOperationException.class, () -> workflowService.extendSLA(1L, req));
        }
    }

    // ===================== FEATURE 5: REASSIGN ROLE =====================

    @Nested class ReassignRole {
        @Test void reassign_success() {
            WorkflowStage stage = WorkflowStage.builder().stageId(3L).caseId(1L).sequenceNumber(3)
                .roleResponsible("CLERK").slaDays(5).stageName("Scheduling")
                .startedAt(LocalDateTime.now()).active(false).skipped(false).build();

            when(workflowStageRepository.findById(3L)).thenReturn(Optional.of(stage));

            ReassignRoleRequest req = new ReassignRoleRequest();
            req.setStageId(3L); req.setNewRole("JUDGE");

            WorkflowStageResponse result = workflowService.reassignRole(1L, req);

            assertEquals("JUDGE", result.getRoleResponsible());
            assertEquals("CLERK", result.getPreviousRole());
        }

        @Test void reassign_completedStage_throws() {
            WorkflowStage stage = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now().minusDays(3)).completedAt(LocalDateTime.now())
                .active(false).skipped(false).build();

            when(workflowStageRepository.findById(1L)).thenReturn(Optional.of(stage));

            ReassignRoleRequest req = new ReassignRoleRequest();
            req.setStageId(1L); req.setNewRole("JUDGE");

            assertThrows(InvalidOperationException.class, () -> workflowService.reassignRole(1L, req));
        }

        @Test void reassign_sameRole_throws() {
            WorkflowStage stage = WorkflowStage.builder().stageId(2L).caseId(1L).sequenceNumber(2)
                .roleResponsible("CLERK").slaDays(5).stageName("Verification")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();

            when(workflowStageRepository.findById(2L)).thenReturn(Optional.of(stage));

            ReassignRoleRequest req = new ReassignRoleRequest();
            req.setStageId(2L); req.setNewRole("CLERK");

            assertThrows(InvalidOperationException.class, () -> workflowService.reassignRole(1L, req));
        }

        @Test void reassign_wrongCase_throws() {
            WorkflowStage stage = WorkflowStage.builder().stageId(5L).caseId(2L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();

            when(workflowStageRepository.findById(5L)).thenReturn(Optional.of(stage));

            ReassignRoleRequest req = new ReassignRoleRequest();
            req.setStageId(5L); req.setNewRole("JUDGE");

            assertThrows(InvalidOperationException.class, () -> workflowService.reassignRole(1L, req));
        }

        @Test void reassign_skippedStage_throws() {
            WorkflowStage stage = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(false).skipped(true)
                .skipReason("not needed").build();

            when(workflowStageRepository.findById(1L)).thenReturn(Optional.of(stage));

            ReassignRoleRequest req = new ReassignRoleRequest();
            req.setStageId(1L); req.setNewRole("JUDGE");

            assertThrows(InvalidOperationException.class, () -> workflowService.reassignRole(1L, req));
        }
    }

    // ===================== ADVANCE WORKFLOW =====================

    @Nested class AdvanceWorkflow {
        @Test void advance_noActiveStage_logsWarning() {
            when(workflowStageRepository.findByCaseIdAndActiveTrue(99L)).thenReturn(Optional.empty());
            // should not throw, just log and return
            assertDoesNotThrow(() -> workflowService.advanceWorkflow(99L));
        }

        @Test void advance_movesToNextStage() {
            WorkflowStage current = WorkflowStage.builder().stageId(1L).caseId(1L).sequenceNumber(1)
                .roleResponsible("CLERK").slaDays(7).stageName("Intake")
                .startedAt(LocalDateTime.now()).active(true).skipped(false).build();
            WorkflowStage next = WorkflowStage.builder().stageId(2L).caseId(1L).sequenceNumber(2)
                .roleResponsible("JUDGE").slaDays(14).stageName("Trial")
                .startedAt(LocalDateTime.now()).active(false).skipped(false).build();

            when(workflowStageRepository.findByCaseIdAndActiveTrue(1L)).thenReturn(Optional.of(current));
            when(slaRecordRepository.findByStageId(1L)).thenReturn(Optional.of(
                SLARecord.builder().slaRecordId(1L).caseId(1L).stageId(1L)
                    .startDate(LocalDate.now()).slaDays(7).status(SLARecord.SLAStatus.ON_TIME)
                    .breachNotified(false).warningNotified(false).build()));
            when(workflowStageRepository.findByCaseIdAndSequenceNumber(1L, 2)).thenReturn(Optional.of(next));
            when(slaRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            workflowService.advanceWorkflow(1L);

            assertFalse(current.getActive());
            assertNotNull(current.getCompletedAt());
            verify(workflowStageRepository, atLeast(2)).save(any());
        }
    }
}
