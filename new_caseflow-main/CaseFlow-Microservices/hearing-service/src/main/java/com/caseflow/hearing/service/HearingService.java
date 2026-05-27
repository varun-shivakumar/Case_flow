package com.caseflow.hearing.service;

import com.caseflow.hearing.client.*;
import com.caseflow.hearing.dto.*;
import com.caseflow.hearing.entity.*;
import com.caseflow.hearing.exception.*;
import com.caseflow.hearing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service @RequiredArgsConstructor @Slf4j
public class HearingService {
    private final HearingRepository hearingRepository;
    private final CaseServiceClient caseClient;
    private final IamServiceClient iamClient;
    private final WorkflowServiceClient workflowClient;
    private final NotificationServiceClient notificationClient;

    public HearingResponse scheduleHearing(HearingRequest request) {
        Hearing hearing = Hearing.builder()
            .caseId(request.getCaseId())
            .judgeId(request.getJudgeId())
            .hearingDate(request.getHearingDate())
            .hearingTime(request.getHearingTime())
            .status(Hearing.HearingStatus.SCHEDULED)
            .scheduledBy(request.getScheduledBy())
            .build();
        Hearing saved = hearingRepository.save(hearing);

        try { workflowClient.advanceWorkflow(hearing.getCaseId()); } catch (Exception e) { log.warn("Failed to advance workflow: {}", e.getMessage()); }

        // Per spec: HEARING SCHEDULED → notify Litigant, Lawyer, Judge.
        String msg = "Hearing #" + saved.getHearingId() + " scheduled for case #" + saved.getCaseId()
            + " on " + saved.getHearingDate() + " at " + saved.getHearingTime() + ".";
        notifyHearingParties(saved, msg);

        return mapToHearingResponse(saved);
    }

    public HearingResponse rescheduleHearing(Long hearingId, RescheduleRequest request) {
        Hearing hearing = hearingRepository.findById(hearingId)
            .orElseThrow(() -> new ResourceNotFoundException("Hearing not found: " + hearingId));
        if (hearing.getStatus() == Hearing.HearingStatus.COMPLETED)
            throw new InvalidOperationException("Cannot reschedule a COMPLETED hearing");

        hearing.setHearingDate(request.getNewDate());
        hearing.setHearingTime(request.getNewTime());
        hearing.setStatus(Hearing.HearingStatus.RESCHEDULED);
        hearing.setRescheduleReason(request.getRescheduleReason());
        Hearing saved = hearingRepository.save(hearing);

        try { caseClient.updateCaseStatusInternal(hearing.getCaseId(), "ADJOURNED"); } catch (Exception e) { log.warn("Failed to update case status: {}", e.getMessage()); }

        // Per spec: HEARING RESCHEDULED → notify Litigant, Lawyer, Judge.
        String msg = "Hearing #" + hearingId + " for case #" + saved.getCaseId()
            + " has been rescheduled to " + saved.getHearingDate() + " at " + saved.getHearingTime()
            + ". Reason: " + saved.getRescheduleReason();
        notifyHearingParties(saved, msg);

        return mapToHearingResponse(saved);
    }

    public HearingResponse completeHearing(Long hearingId, CompleteHearingRequest request) {
        Hearing hearing = hearingRepository.findById(hearingId)
            .orElseThrow(() -> new ResourceNotFoundException("Hearing not found: " + hearingId));
        if (hearing.getStatus() == Hearing.HearingStatus.COMPLETED)
            throw new InvalidOperationException("Hearing already COMPLETED");
        if (!hearing.getJudgeId().equals(request.getJudgeId()))
            throw new InvalidOperationException("Judge not assigned to this hearing");
        hearing.setStatus(Hearing.HearingStatus.COMPLETED);
        hearing.setHearingNotes(request.getHearingNotes());
        Hearing saved = hearingRepository.save(hearing);

        try { workflowClient.advanceWorkflow(hearing.getCaseId()); } catch (Exception e) { log.warn("Failed to advance workflow: {}", e.getMessage()); }

        // Hearing complete is also relevant to the litigant + lawyer (they may want
        // to know the proceedings happened and a decision is forthcoming).
        notifyHearingParties(saved,
            "Hearing #" + hearingId + " for case #" + saved.getCaseId()
            + " has been marked COMPLETED. Workflow advanced.");

        return mapToHearingResponse(saved);
    }

    public HearingResponse getHearingById(Long id) {
        return mapToHearingResponse(hearingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hearing not found: " + id)));
    }
    public List<HearingResponse> getAllHearings() { return hearingRepository.findAll().stream().map(this::mapToHearingResponse).toList(); }
    public List<HearingResponse> getHearingsByCase(Long id) { return hearingRepository.findByCaseId(id).stream().map(this::mapToHearingResponse).toList(); }
    public List<HearingResponse> getHearingsByJudge(String id) { return hearingRepository.findByJudgeId(id).stream().map(this::mapToHearingResponse).toList(); }
    public List<HearingResponse> getHearingsByStatus(Hearing.HearingStatus s) { return hearingRepository.findByStatus(s).stream().map(this::mapToHearingResponse).toList(); }

    private void sendNotification(String userId, Long caseId, String message, String category) {
        if (userId == null || userId.isBlank()) return;
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("userId", userId);
            req.put("caseId", caseId);
            req.put("message", message);
            req.put("category", category);
            notificationClient.sendNotification(req);
        } catch (Exception e) {
            log.warn("Notification failed for case #{}: {}", caseId, e.getMessage());
        }
    }

    /** Fan out a hearing notification to Judge + Litigant + Lawyer for the case. */
    private void notifyHearingParties(Hearing h, String message) {
        // Judge — always known on the hearing itself.
        sendNotification(h.getJudgeId(), h.getCaseId(), message, "HEARING");
        // Litigant + Lawyer — looked up from the case so we don't depend on the caller.
        try {
            var c = caseClient.getCaseById(h.getCaseId());
            if (c != null) {
                sendNotification(c.getLitigantId(), h.getCaseId(), message, "HEARING");
                if (c.getLawyerId() != null && !c.getLawyerId().isBlank()) {
                    sendNotification(c.getLawyerId(), h.getCaseId(), message, "HEARING");
                }
            }
        } catch (Exception e) {
            log.warn("Could not load case #{} for hearing fan-out: {}", h.getCaseId(), e.getMessage());
        }
    }

    private HearingResponse mapToHearingResponse(Hearing h) {
        HearingResponse r = new HearingResponse();
        r.setHearingId(h.getHearingId()); r.setCaseId(h.getCaseId());
        r.setJudgeId(h.getJudgeId()); r.setHearingDate(h.getHearingDate());
        r.setHearingTime(h.getHearingTime()); r.setStatus(h.getStatus());
        r.setScheduledBy(h.getScheduledBy()); r.setRescheduleReason(h.getRescheduleReason());
        r.setHearingNotes(h.getHearingNotes()); return r;
    }
}
