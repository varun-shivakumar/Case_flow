package com.caseflow.workflow.controller;

import com.caseflow.workflow.dto.*;
import com.caseflow.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequiredArgsConstructor
@Tag(name = "Case Lifecycle & Workflow",
     description = "Module 4.4 — Workflow stages, SLA tracking, lifecycle management, "
         + "rollback, skip, SLA extension, role reassignment, 80% early warning")
public class WorkflowController {
    private final WorkflowService workflowService;

    // ===================== EXISTING ENDPOINTS =====================

    @PostMapping("/api/workflow/lifecycle/{caseId}/initialize")
    @Operation(summary = "Initialize workflow lifecycle for a case",
        description = "Modes: 'auto' (uses template based on caseType) or 'manual' (provide custom stages). "
            + "Case types for auto: civil, criminal, corporate")
    public ResponseEntity<Map<String, Object>> initLifecycle(@PathVariable Long caseId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id", required = false) String userId,
            @Valid @RequestBody(required = false) LifecycleInitRequest request) {
        if (request == null) request = new LifecycleInitRequest();
        String requestedBy = (userId != null && !userId.isBlank()) ? userId : "SYSTEM";
        workflowService.initLifecycle(caseId, request.getMode(), request.getCaseType(), request.getStages(), requestedBy);
        return ResponseEntity.ok(Map.of("message", "Lifecycle initialized for Case " + caseId,
            "mode", request.getMode(), "caseType", request.getCaseType()));
    }

    @GetMapping("/api/workflow/cases/{caseId}/stages")
    @Operation(summary = "Get all workflow stages for a case")
    public ResponseEntity<List<WorkflowStageResponse>> getStagesByCase(@PathVariable Long caseId) {
        return ResponseEntity.ok(workflowService.getStagesByCaseId(caseId));
    }

    @GetMapping("/api/workflow/cases/{caseId}/stages/current")
    @Operation(summary = "Get the currently active workflow stage")
    public ResponseEntity<WorkflowStageResponse> getCurrentStage(@PathVariable Long caseId) {
        return ResponseEntity.ok(workflowService.getCurrentStage(caseId));
    }

    @PostMapping("/api/workflow/cases/{caseId}/advance")
    @Operation(summary = "Advance workflow to the next stage")
    public ResponseEntity<Map<String, String>> advanceWorkflow(@PathVariable Long caseId) {
        workflowService.advanceWorkflow(caseId);
        return ResponseEntity.ok(Map.of("message", "Workflow advanced for Case: " + caseId));
    }

    @GetMapping("/api/workflow/cases/{caseId}/sla")
    @Operation(summary = "Get all SLA records for a case")
    public ResponseEntity<List<SLARecordResponse>> getSLAByCase(@PathVariable Long caseId) {
        return ResponseEntity.ok(workflowService.getSLARecordsByCaseId(caseId));
    }

    @GetMapping("/api/workflow/sla/breached")
    @Operation(summary = "Get all breached SLA records across all cases")
    public ResponseEntity<List<SLARecordResponse>> getAllBreached() {
        return ResponseEntity.ok(workflowService.getAllBreachedSLAs());
    }

    @GetMapping("/api/workflow/sla/active")
    @Operation(summary = "Get all currently active (open) SLA records")
    public ResponseEntity<List<SLARecordResponse>> getAllActive() {
        return ResponseEntity.ok(workflowService.getAllActiveSLAs());
    }

    @PostMapping("/api/workflow/sla/check")
    @Operation(summary = "Manually trigger SLA breach & early warning check",
        description = "Checks all active SLAs. Sends WARNING notifications at 80% usage "
            + "and BREACH notifications when SLA is exceeded.")
    public ResponseEntity<String> runSLACheck() {
        return ResponseEntity.ok(workflowService.runManualSLACheck());
    }

    // ===================== NEW: SLA WARNINGS =====================

    @GetMapping("/api/workflow/sla/warnings")
    @Operation(summary = "Get all SLA records currently in WARNING state (80%+ consumed)")
    public ResponseEntity<List<SLARecordResponse>> getAllWarnings() {
        return ResponseEntity.ok(workflowService.getAllWarningSLAs());
    }

    // ===================== NEW: ROLLBACK =====================

    @PostMapping("/api/workflow/cases/{caseId}/rollback")
    @Operation(summary = "Rollback workflow to the previous stage",
        description = "Deactivates current stage and re-activates the previous non-skipped stage. "
            + "A fresh SLA timer is created for the rolled-back stage. "
            + "Cannot rollback if already at Stage 1.")
    public ResponseEntity<WorkflowStageResponse> rollbackWorkflow(@PathVariable Long caseId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id", required = false) String userId) {
        String requestedBy = (userId != null && !userId.isBlank()) ? userId : "SYSTEM";
        return ResponseEntity.ok(workflowService.rollbackWorkflow(caseId, requestedBy));
    }

    // ===================== NEW: SKIP STAGE =====================

    @PostMapping("/api/workflow/cases/{caseId}/skip")
    @Operation(summary = "Skip the current workflow stage with a reason",
        description = "Marks current stage as SKIPPED (not completed), stores the skip reason, "
            + "and advances to the next stage. The reason is preserved for audit/display purposes.")
    public ResponseEntity<WorkflowStageResponse> skipCurrentStage(@PathVariable Long caseId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id", required = false) String userId,
            @Valid @RequestBody SkipStageRequest request) {
        String requestedBy = (userId != null && !userId.isBlank()) ? userId : "SYSTEM";
        return ResponseEntity.ok(workflowService.skipCurrentStage(caseId, request.getReason(), requestedBy));
    }

    // ===================== NEW: SLA EXTENSION =====================

    @PatchMapping("/api/workflow/cases/{caseId}/sla/extend")
    @Operation(summary = "Extend the SLA deadline for the current active stage",
        description = "Adds extra days to the current stage's SLA. Stores the original SLA "
            + "and extension reason. If the stage was BREACHED or WARNING, "
            + "status is re-evaluated with the new deadline.")
    public ResponseEntity<SLARecordResponse> extendSLA(@PathVariable Long caseId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id", required = false) String userId,
            @Valid @RequestBody SLAExtensionRequest request) {
        String requestedBy = (userId != null && !userId.isBlank()) ? userId : "SYSTEM";
        return ResponseEntity.ok(workflowService.extendSLA(caseId, request, requestedBy));
    }

    // ===================== NEW: REASSIGN ROLE =====================

    @PatchMapping("/api/workflow/cases/{caseId}/reassign")
    @Operation(summary = "Reassign the role responsible for a stage",
        description = "Changes the roleResponsible for a specific stage. "
            + "Can only reassign stages that are active or haven't started yet. "
            + "Cannot reassign completed or skipped stages. "
            + "Stores the previous role for audit trail.")
    public ResponseEntity<WorkflowStageResponse> reassignRole(@PathVariable Long caseId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id", required = false) String userId,
            @Valid @RequestBody ReassignRoleRequest request) {
        String requestedBy = (userId != null && !userId.isBlank()) ? userId : "SYSTEM";
        return ResponseEntity.ok(workflowService.reassignRole(caseId, request, requestedBy));
    }
}
