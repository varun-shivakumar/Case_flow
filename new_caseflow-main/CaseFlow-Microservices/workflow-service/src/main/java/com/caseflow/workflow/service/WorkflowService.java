package com.caseflow.workflow.service;

import com.caseflow.workflow.client.*;
import com.caseflow.workflow.dto.*;
import com.caseflow.workflow.entity.*;
import com.caseflow.workflow.exception.*;
import com.caseflow.workflow.repository.*;
import com.caseflow.workflow.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j @Service @RequiredArgsConstructor
public class WorkflowService {
    private final WorkflowStageRepository workflowStageRepository;
    private final SLARecordRepository slaRecordRepository;
    private final CaseServiceClient caseClient;
    private final NotificationServiceClient notificationClient;
    private final CachedWorkflowService cachedWorkflowService;
    private final IamServiceClient iamClient;

    // ===================== LIFECYCLE INIT =====================

    @Transactional
    public void initLifecycle(Long caseId, String mode, String caseType,
                              List<ManualStageRequest> manualStages, String requestedBy) {
        try { caseClient.setCaseType(caseId, caseType); } catch (Exception e) {
            log.warn("Failed to set case type via case-service: {}", e.getMessage());
        }
        if ("manual".equalsIgnoreCase(mode)) {
            if (manualStages == null || manualStages.isEmpty())
                throw new InvalidOperationException("Manual mode requires stages list");
            initManualStages(caseId, manualStages);
        } else {
            List<StageDefinition> stages;
            if ("criminal".equalsIgnoreCase(caseType)) stages = StageTemplate.getCriminalStage();
            else if ("corporate".equalsIgnoreCase(caseType)) stages = StageTemplate.getCorporateStage();
            else stages = StageTemplate.getCivilStage();
            initFromTemplate(caseId, stages);
        }

        advanceWorkflow(caseId);


        sendNotification(requestedBy, caseId, "Workflow lifecycle initialized for Case #" + caseId
                + " (Type: " + caseType + ", Mode: " + mode + ")", "CASE");

        log.info("Lifecycle initialized for Case: {} Mode: {} Type: {}", caseId, mode, caseType);
    }

    private void initFromTemplate(Long caseId, List<StageDefinition> stages) {
        List<WorkflowStage> toSave = new ArrayList<>();
        for (StageDefinition stg : stages) {
            toSave.add(WorkflowStage.builder().caseId(caseId).sequenceNumber(stg.getSeqNum())
                .roleResponsible(stg.getRole().toUpperCase()).slaDays(stg.getSlaDays())
                .stageName(stg.getStageName()).startedAt(LocalDateTime.now())
                .active(stg.getSeqNum() == 1).skipped(false).build());
        }
        workflowStageRepository.saveAll(toSave);
        workflowStageRepository.flush();
        WorkflowStage first = workflowStageRepository.findByCaseIdAndSequenceNumber(caseId, 1)
            .orElseThrow(() -> new ResourceNotFoundException("First stage not found after save"));
        slaRecordRepository.save(SLARecord.builder().caseId(caseId).stageId(first.getStageId())
            .startDate(LocalDate.now()).status(SLARecord.SLAStatus.ON_TIME)
            .slaDays(first.getSlaDays()).breachNotified(false).warningNotified(false).build());
    }

    private void initManualStages(Long caseId, List<ManualStageRequest> manualStages) {
        List<WorkflowStage> toSave = new ArrayList<>();
        Set<Integer> seqSet = new HashSet<>();
        for (ManualStageRequest m : manualStages) {
            if (m.getSequenceNumber() <= 0) throw new IllegalArgumentException("Sequence number must be >= 1");
            if (!seqSet.add(m.getSequenceNumber())) throw new IllegalArgumentException("Duplicate sequence number");
            if (m.getSlaDays() <= 0) throw new IllegalArgumentException("SLA days must be >= 1");
            toSave.add(WorkflowStage.builder().caseId(caseId).sequenceNumber(m.getSequenceNumber())
                .roleResponsible(m.getRoleResponsible().toUpperCase()).slaDays(m.getSlaDays())
                .stageName(m.getStageName()).startedAt(LocalDateTime.now())
                .active(m.getSequenceNumber() == 1).skipped(false).build());
        }
        toSave.sort(Comparator.comparingInt(WorkflowStage::getSequenceNumber));
        workflowStageRepository.saveAll(toSave);
        workflowStageRepository.flush();
        WorkflowStage first = workflowStageRepository.findByCaseIdAndSequenceNumber(caseId, 1)
            .orElseThrow(() -> new ResourceNotFoundException("First stage not found after save"));
        slaRecordRepository.save(SLARecord.builder().caseId(caseId).stageId(first.getStageId())
            .startDate(LocalDate.now()).status(SLARecord.SLAStatus.ON_TIME)
            .slaDays(first.getSlaDays()).breachNotified(false).warningNotified(false).build());
    }

    // ===================== ADVANCE WORKFLOW =====================

    public void advanceWorkflow(Long caseId) {
        WorkflowStage current = workflowStageRepository.findByCaseIdAndActiveTrue(caseId).orElse(null);
        if (current == null) { log.warn("No active workflow stage found for case: {}", caseId); return; }

        current.setActive(false);
        current.setCompletedAt(LocalDateTime.now());
        workflowStageRepository.save(current);

        closeSLAForStage(current.getStageId());

        int nextSeq = current.getSequenceNumber() + 1;
        Optional<WorkflowStage> next = workflowStageRepository.findByCaseIdAndSequenceNumber(caseId, nextSeq);
        if (next.isPresent()) {
            activateStage(next.get());
            sendNotification("SYSTEM", caseId, "Case #" + caseId + " advanced to Stage "
                    + nextSeq + ": " + next.get().getStageName(), "CASE");
        } else {
            sendNotification("SYSTEM", caseId, "Workflow completed for Case #" + caseId
                    + ". All stages finished.", "CASE");
            log.info("Workflow completed for Case: {}", caseId);
        }
        log.info("Workflow advanced for Case: {} -> Stage {}", caseId, nextSeq);
    }

    // ===================== FEATURE 1: SLA EARLY WARNING (80%) =====================

    public String runManualSLACheck() {
        List<SLARecord> active = slaRecordRepository.findAll().stream()
                .filter(s -> s.getEndDate() == null).toList();
        int breachCount = 0;
        int warningCount = 0;

        for (SLARecord sla : active) {
            long elapsed = ChronoUnit.DAYS.between(sla.getStartDate(), LocalDate.now());
            double usagePercent = (double) elapsed / sla.getSlaDays() * 100;

            // 80% early warning check
            if (usagePercent >= 80 && usagePercent < 100
                    && !Boolean.TRUE.equals(sla.getWarningNotified())
                    && sla.getStatus() != SLARecord.SLAStatus.BREACHED) {
                sla.setStatus(SLARecord.SLAStatus.WARNING);
                sla.setWarningNotified(true);
                slaRecordRepository.save(sla);

                // Per spec: SLA early-warning escalates to Court Administrator (ADMIN role).
                notifyAllByRole("ADMIN", sla.getCaseId(),
                        "SLA WARNING: Case #" + sla.getCaseId() + " Stage #" + sla.getStageId()
                        + " has consumed " + Math.round(usagePercent) + "% of SLA ("
                        + elapsed + "/" + sla.getSlaDays() + " days). Action needed soon!",
                        "COMPLIANCE");

                log.warn("SLA WARNING — Case: {} Stage: {} Usage: {}%",
                        sla.getCaseId(), sla.getStageId(), Math.round(usagePercent));
                warningCount++;
            }

            // breach check
            if (elapsed > sla.getSlaDays() && sla.getStatus() != SLARecord.SLAStatus.BREACHED) {
                sla.setStatus(SLARecord.SLAStatus.BREACHED);
                sla.setBreachNotified(true);
                slaRecordRepository.save(sla);

                // Per spec: SLA BREACHED → escalation notification to Court Administrator(s).
                notifyAllByRole("ADMIN", sla.getCaseId(),
                        "SLA BREACHED: Case #" + sla.getCaseId() + " Stage #" + sla.getStageId()
                        + " exceeded SLA by " + (elapsed - sla.getSlaDays()) + " day(s).",
                        "COMPLIANCE");

                breachCount++;
            }
        }
        return "SLA check completed. Warnings: " + warningCount
                + ", Breaches: " + breachCount + " out of " + active.size() + " active records.";
    }

    // ===================== FEATURE 2: ROLLBACK WORKFLOW =====================

    @Transactional
    public WorkflowStageResponse rollbackWorkflow(Long caseId, String requestedBy) {
        WorkflowStage current = workflowStageRepository.findByCaseIdAndActiveTrue(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("No active stage found for case: " + caseId));

        if (current.getSequenceNumber() <= 1) {
            throw new InvalidOperationException("Cannot rollback — already at the first stage");
        }

        // deactivate current stage, reset its timestamps
        current.setActive(false);
        current.setStartedAt(LocalDateTime.now()); // reset so it's fresh when re-entered later
        current.setCompletedAt(null);
        workflowStageRepository.save(current);

        // close current SLA as ROLLED_BACK
        closeSLAForStage(current.getStageId(), SLARecord.SLAStatus.ROLLED_BACK);

        // find previous stage (skip over any skipped stages)
        int prevSeq = current.getSequenceNumber() - 1;
        WorkflowStage previous = null;
        while (prevSeq >= 1) {
            Optional<WorkflowStage> opt = workflowStageRepository.findByCaseIdAndSequenceNumber(caseId, prevSeq);
            if (opt.isPresent() && !Boolean.TRUE.equals(opt.get().getSkipped())) {
                previous = opt.get();
                break;
            }
            prevSeq--;
        }

        if (previous == null) {
            throw new InvalidOperationException("No non-skipped previous stage available to rollback to");
        }

        // re-activate previous stage
        previous.setActive(true);
        previous.setCompletedAt(null);
        previous.setStartedAt(LocalDateTime.now());
        workflowStageRepository.save(previous);

        // create fresh SLA for the rolled-back stage
        slaRecordRepository.save(SLARecord.builder().caseId(caseId).stageId(previous.getStageId())
                .startDate(LocalDate.now()).status(SLARecord.SLAStatus.ON_TIME)
                .slaDays(previous.getSlaDays()).breachNotified(false).warningNotified(false).build());

        sendNotification(requestedBy, caseId, "Workflow for Case #" + caseId
                + " rolled back from Stage " + current.getSequenceNumber()
                + " (" + current.getStageName() + ") to Stage " + previous.getSequenceNumber()
                + " (" + previous.getStageName() + ")", "CASE");

        log.info("Workflow rolled back for Case: {} from Stage {} to Stage {}",
                caseId, current.getSequenceNumber(), previous.getSequenceNumber());

        return mapToStageResponse(previous);
    }

    // ===================== FEATURE 3: SKIP STAGE =====================

    @Transactional
    public WorkflowStageResponse skipCurrentStage(Long caseId, String reason, String requestedBy) {
        WorkflowStage current = workflowStageRepository.findByCaseIdAndActiveTrue(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("No active stage found for case: " + caseId));

        // mark as skipped
        current.setActive(false);
        current.setSkipped(true);
        current.setSkipReason(reason);
        current.setCompletedAt(LocalDateTime.now());
        workflowStageRepository.save(current);

        // close the SLA for the skipped stage as SKIPPED
        closeSLAForStage(current.getStageId(), SLARecord.SLAStatus.SKIPPED);

        // find and activate next stage
        int nextSeq = current.getSequenceNumber() + 1;
        Optional<WorkflowStage> next = workflowStageRepository.findByCaseIdAndSequenceNumber(caseId, nextSeq);

        if (next.isPresent()) {
            activateStage(next.get());

            sendNotification(requestedBy, caseId, "Stage " + current.getSequenceNumber()
                    + " (" + current.getStageName() + ") SKIPPED for Case #" + caseId
                    + ". Reason: " + reason + ". Moved to Stage " + nextSeq
                    + " (" + next.get().getStageName() + ")", "CASE");

            log.info("Stage {} skipped for Case: {}, reason: {}", current.getSequenceNumber(), caseId, reason);
            return mapToStageResponse(next.get());
        } else {
            sendNotification(requestedBy, caseId, "Last stage skipped for Case #" + caseId
                    + ". Workflow completed. Skip reason: " + reason, "CASE");
            log.info("Last stage skipped, workflow completed for Case: {}", caseId);
            return mapToStageResponse(current);
        }
    }

    // ===================== FEATURE 4: SLA EXTENSION / DEADLINE OVERRIDE =====================

    @Transactional
    public SLARecordResponse extendSLA(Long caseId, SLAExtensionRequest request, String requestedBy) {
        WorkflowStage current = workflowStageRepository.findByCaseIdAndActiveTrue(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("No active stage found for case: " + caseId));

        SLARecord sla = slaRecordRepository.findByStageIdAndEndDateIsNull(current.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No open SLA record found for active stage of case: " + caseId));

        if (sla.getEndDate() != null) {
            throw new InvalidOperationException("Cannot extend SLA for a completed/closed stage");
        }

        // store original if this is the first extension
        if (sla.getOriginalSlaDays() == null) {
            sla.setOriginalSlaDays(sla.getSlaDays());
        }

        int oldDays = sla.getSlaDays();
        sla.setSlaDays(oldDays + request.getAdditionalDays());
        sla.setExtensionReason(request.getReason());

        // Also sync workflow_stages.sla_days so stage queries reflect the updated SLA
        current.setSlaDays(sla.getSlaDays());
        workflowStageRepository.save(current);

        // if it was BREACHED or WARNING, re-evaluate status with new deadline
        long elapsed = ChronoUnit.DAYS.between(sla.getStartDate(), LocalDate.now());
        if (elapsed <= sla.getSlaDays()) {
            double usagePercent = (double) elapsed / sla.getSlaDays() * 100;
            if (usagePercent >= 80) {
                sla.setStatus(SLARecord.SLAStatus.WARNING);
            } else {
                sla.setStatus(SLARecord.SLAStatus.ON_TIME);
                sla.setWarningNotified(false); // reset warning since we have breathing room
            }
            sla.setBreachNotified(false);
        }

        slaRecordRepository.save(sla);
        cachedWorkflowService.evictSLACacheForStage(sla.getStageId());

        sendNotification(requestedBy, caseId, "SLA Extended for Case #" + caseId + " Stage "
                + current.getSequenceNumber() + " (" + current.getStageName() + "): "
                + oldDays + " → " + sla.getSlaDays() + " days. Reason: " + request.getReason(),
                "COMPLIANCE");

        log.info("SLA extended for Case: {} Stage: {} from {} to {} days. Reason: {}",
                caseId, current.getStageId(), oldDays, sla.getSlaDays(), request.getReason());

        return mapToSLAResponse(sla);
    }

    // ===================== FEATURE 5: REASSIGN ROLE =====================

    @Transactional
    public WorkflowStageResponse reassignRole(Long caseId, ReassignRoleRequest request, String requestedBy) {
        WorkflowStage stage = workflowStageRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found: " + request.getStageId()));

        if (!stage.getCaseId().equals(caseId)) {
            throw new InvalidOperationException("Stage #" + request.getStageId()
                    + " does not belong to Case #" + caseId);
        }

        // can only reassign stages that haven't started yet OR the currently active one
        if (stage.getCompletedAt() != null || Boolean.TRUE.equals(stage.getSkipped())) {
            throw new InvalidOperationException("Cannot reassign a completed or skipped stage");
        }

        String oldRole = stage.getRoleResponsible();
        String newRole = request.getNewRole().toUpperCase();

        if (oldRole.equals(newRole)) {
            throw new InvalidOperationException("New role is the same as current role: " + oldRole);
        }

        stage.setPreviousRole(oldRole);
        stage.setRoleResponsible(newRole);
        workflowStageRepository.save(stage);

        sendNotification(requestedBy, caseId, "Role reassigned for Case #" + caseId + " Stage "
                + stage.getSequenceNumber() + " (" + stage.getStageName() + "): "
                + oldRole + " → " + newRole, "CASE");

        log.info("Role reassigned for Case: {} Stage: {} from {} to {}",
                caseId, stage.getStageId(), oldRole, newRole);

        return mapToStageResponse(stage);
    }

    // ===================== QUERIES =====================

    public List<WorkflowStageResponse> getStagesByCaseId(Long caseId) {
        return workflowStageRepository.findByCaseIdOrderBySequenceNumber(caseId)
                .stream().map(this::mapToStageResponse).toList();
    }

    public WorkflowStageResponse getCurrentStage(Long caseId) {
        return mapToStageResponse(workflowStageRepository.findByCaseIdAndActiveTrue(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("No active stage found for case: " + caseId)));
    }

    public List<SLARecordResponse> getSLARecordsByCaseId(Long caseId) {
        return slaRecordRepository.findByCaseId(caseId).stream().map(this::mapToSLAResponse).toList();
    }

    public List<SLARecordResponse> getAllBreachedSLAs() {
        return slaRecordRepository.findByStatus(SLARecord.SLAStatus.BREACHED)
                .stream().map(this::mapToSLAResponse).toList();
    }

    public List<SLARecordResponse> getAllWarningSLAs() {
        return slaRecordRepository.findByStatus(SLARecord.SLAStatus.WARNING)
                .stream().map(this::mapToSLAResponse).toList();
    }

    public List<SLARecordResponse> getAllActiveSLAs() {
        return slaRecordRepository.findAll().stream()
                .filter(s -> s.getEndDate() == null).map(this::mapToSLAResponse).toList();
    }

    // ===================== INTERNAL HELPERS =====================

    private void closeSLAForStage(Long stageId) {
        closeSLAForStage(stageId, null);
    }

    private void closeSLAForStage(Long stageId, SLARecord.SLAStatus overrideStatus) {
        SLARecord sla = slaRecordRepository.findByStageIdAndEndDateIsNull(stageId).orElse(null);
        if (sla != null && sla.getEndDate() == null) {
            sla.setEndDate(LocalDate.now());
            if (overrideStatus != null) {
                sla.setStatus(overrideStatus);
            } else {
                long elapsed = ChronoUnit.DAYS.between(sla.getStartDate(), LocalDate.now());
                if (sla.getStatus() != SLARecord.SLAStatus.BREACHED) {
                    sla.setStatus(elapsed <= sla.getSlaDays()
                            ? SLARecord.SLAStatus.COMPLETED : SLARecord.SLAStatus.BREACHED);
                }
            }
            slaRecordRepository.save(sla);
        }
    }

    private void activateStage(WorkflowStage stage) {
        stage.setActive(true);
        stage.setStartedAt(LocalDateTime.now());
        stage.setCompletedAt(null);
        workflowStageRepository.save(stage);

        slaRecordRepository.save(SLARecord.builder()
                .caseId(stage.getCaseId()).stageId(stage.getStageId())
                .startDate(LocalDate.now()).status(SLARecord.SLAStatus.ON_TIME)
                .slaDays(stage.getSlaDays()).breachNotified(false)
                .warningNotified(false).build());
    }

    private void sendNotification(String userId, Long caseId, String message, String category) {
        if (userId == null || userId.isBlank()) return;
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("userId",   userId);
            req.put("caseId",   caseId);
            req.put("message",  message);
            req.put("category", category);
            notificationClient.sendNotification(req);
        } catch (Exception e) {
            log.warn("Notification failed for Case {}: {}", caseId, e.getMessage());
        }
    }

    /** Fan out a notification to every active user with the given role (e.g. ADMIN). */
    private void notifyAllByRole(String role, Long caseId, String message, String category) {
        try {
            var recipients = iamClient.getUsersByRole(role);
            if (recipients == null) return;
            for (var u : recipients) {
                if (u == null || u.getUserId() == null) continue;
                if (u.getStatus() != null && !"ACTIVE".equalsIgnoreCase(u.getStatus())) continue;
                sendNotification(u.getUserId(), caseId, message, category);
            }
        } catch (Exception e) {
            log.warn("Could not fan out notification to role {}: {}", role, e.getMessage());
        }
    }

    // ===================== MAPPERS =====================

    private WorkflowStageResponse mapToStageResponse(WorkflowStage s) {
        WorkflowStageResponse r = new WorkflowStageResponse();
        r.setStageId(s.getStageId()); r.setCaseId(s.getCaseId());
        r.setSequenceNumber(s.getSequenceNumber()); r.setRoleResponsible(s.getRoleResponsible());
        r.setSlaDays(s.getSlaDays()); r.setStageName(s.getStageName());
        r.setStartedAt(s.getStartedAt()); r.setCompletedAt(s.getCompletedAt());
        r.setActive(s.getActive()); r.setSkipped(s.getSkipped());
        r.setSkipReason(s.getSkipReason()); r.setPreviousRole(s.getPreviousRole());
        return r;
    }

    private SLARecordResponse mapToSLAResponse(SLARecord s) {
        SLARecordResponse r = new SLARecordResponse();
        r.setSlaRecordId(s.getSlaRecordId()); r.setCaseId(s.getCaseId());
        r.setStageId(s.getStageId()); r.setStartDate(s.getStartDate());
        r.setEndDate(s.getEndDate()); r.setStatus(s.getStatus());
        r.setSlaDays(s.getSlaDays()); r.setBreachNotified(s.getBreachNotified());
        r.setWarningNotified(s.getWarningNotified());
        r.setOriginalSlaDays(s.getOriginalSlaDays());
        r.setExtensionReason(s.getExtensionReason());
        LocalDate end = s.getEndDate() != null ? s.getEndDate() : LocalDate.now();
        long elapsed = ChronoUnit.DAYS.between(s.getStartDate(), end);
        r.setDaysElapsed(elapsed);
        r.setDaysRemaining(s.getSlaDays() - elapsed);
        r.setSlaUsagePercent(s.getSlaDays() > 0 ? Math.round((double) elapsed / s.getSlaDays() * 10000.0) / 100.0 : 0.0);
        return r;
    }
}
