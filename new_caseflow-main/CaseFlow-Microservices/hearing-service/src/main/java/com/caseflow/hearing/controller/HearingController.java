package com.caseflow.hearing.controller;

import com.caseflow.hearing.dto.*;
import com.caseflow.hearing.entity.Hearing;
import com.caseflow.hearing.service.HearingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api/hearings") @RequiredArgsConstructor
@Tag(name = "Hearing", description = "Manage hearings")
public class HearingController {
    private final HearingService hearingService;

    @PostMapping("/schedule")
    public ResponseEntity<HearingResponse> scheduleHearing(
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @Valid @RequestBody HearingRequest request) {
        denyAdmin(userRole);
        return ResponseEntity.ok(hearingService.scheduleHearing(request));
    }

    @PatchMapping("/{hearingId}/reschedule")
    public ResponseEntity<HearingResponse> rescheduleHearing(
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long hearingId,
            @Valid @RequestBody RescheduleRequest request) {
        denyAdmin(userRole);
        return ResponseEntity.ok(hearingService.rescheduleHearing(hearingId, request));
    }

    @PatchMapping("/{hearingId}/complete")
    public ResponseEntity<HearingResponse> completeHearing(
            @PathVariable Long hearingId,
            @Valid @RequestBody CompleteHearingRequest request) {
        return ResponseEntity.ok(hearingService.completeHearing(hearingId, request));
    }

    @GetMapping("/{hearingId}") public ResponseEntity<HearingResponse> getHearingById(@PathVariable Long hearingId) { return ResponseEntity.ok(hearingService.getHearingById(hearingId)); }
    @GetMapping public ResponseEntity<List<HearingResponse>> getAllHearings() { return ResponseEntity.ok(hearingService.getAllHearings()); }
    @GetMapping("/case/{caseId}") public ResponseEntity<List<HearingResponse>> getHearingsByCase(@PathVariable Long caseId) { return ResponseEntity.ok(hearingService.getHearingsByCase(caseId)); }
    @GetMapping("/judge/{judgeId}") public ResponseEntity<List<HearingResponse>> getHearingsByJudge(@PathVariable String judgeId) { return ResponseEntity.ok(hearingService.getHearingsByJudge(judgeId)); }
    @GetMapping("/status/{status}") public ResponseEntity<List<HearingResponse>> getHearingsByStatus(@PathVariable Hearing.HearingStatus status) { return ResponseEntity.ok(hearingService.getHearingsByStatus(status)); }

    private void denyAdmin(String userRole) {
        if (userRole == null || userRole.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        if ("ADMIN".equalsIgnoreCase(userRole) || "ROLE_ADMIN".equalsIgnoreCase(userRole))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN cannot schedule hearings.");
    }
}
