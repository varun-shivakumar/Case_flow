package com.caseflow.cases.controller;

import com.caseflow.cases.dto.CaseResponse;
import com.caseflow.cases.entity.Case;
import com.caseflow.cases.entity.Document;
import com.caseflow.cases.service.CachedCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases/paginated")
@RequiredArgsConstructor
@Tag(name = "Cases — Paginated", description = "Paginated and cached case endpoints")
public class PaginatedCaseController {

    private final CachedCaseService cachedCaseService;

    @GetMapping
    @Operation(summary = "Get all cases (paginated)")
    public ResponseEntity<Page<Case>> getAllCasesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "caseId,desc") String sort) {
        String[] s = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(s.length > 1 && s[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, s[0]));
        return ResponseEntity.ok(cachedCaseService.getAllCasesPaginated(pageable));
    }

    @GetMapping("/documents")
    @Operation(summary = "Get all documents (paginated)")
    public ResponseEntity<Page<Document>> getAllDocumentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(cachedCaseService.getAllDocumentsPaginated(PageRequest.of(page, size)));
    }

    @GetMapping("/cached/{caseId}")
    @Operation(summary = "Get case by ID (cached)")
    public ResponseEntity<CaseResponse> getCachedCase(@PathVariable Long caseId) {
        return ResponseEntity.ok(cachedCaseService.getCachedCaseById(caseId));
    }
}
