package com.caseflow.hearing.controller;

import com.caseflow.hearing.entity.Hearing;
import com.caseflow.hearing.service.CachedHearingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hearings/paginated")
@RequiredArgsConstructor
@Tag(name = "Hearings — Paginated", description = "Paginated hearing endpoints")
public class PaginatedHearingController {
    private final CachedHearingService cachedHearingService;

    @GetMapping
    @Operation(summary = "Get all hearings (paginated)")
    public ResponseEntity<Page<Hearing>> getAllHearingsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "hearingId,desc") String sort) {
        String[] s = sort.split(",");
        return ResponseEntity.ok(cachedHearingService.getAllHearingsPaginated(
            PageRequest.of(page, size, Sort.by(s.length > 1 && s[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, s[0]))));
    }
}
