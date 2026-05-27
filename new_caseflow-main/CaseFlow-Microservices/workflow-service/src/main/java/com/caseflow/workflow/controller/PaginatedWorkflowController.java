package com.caseflow.workflow.controller;

import com.caseflow.workflow.entity.WorkflowStage;
import com.caseflow.workflow.entity.SLARecord;
import com.caseflow.workflow.service.CachedWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow/paginated")
@RequiredArgsConstructor
@Tag(name = "Workflow — Paginated", description = "Paginated workflow and SLA endpoints")
public class PaginatedWorkflowController {

    private final CachedWorkflowService cachedWorkflowService;

    @GetMapping("/stages")
    @Operation(summary = "Get all workflow stages across all cases (paginated)")
    public ResponseEntity<Page<WorkflowStage>> getAllStagesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "stageId,desc") String sort) {
        String[] s = sort.split(",");
        return ResponseEntity.ok(cachedWorkflowService.getAllStagesPaginated(
            PageRequest.of(page, size, Sort.by(s.length > 1 && s[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, s[0]))));
    }

    @GetMapping("/sla")
    @Operation(summary = "Get all SLA records (paginated)")
    public ResponseEntity<Page<SLARecord>> getAllSLARecordsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(cachedWorkflowService.getAllSLARecordsPaginated(PageRequest.of(page, size)));
    }
}
