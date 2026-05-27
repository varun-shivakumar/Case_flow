package com.caseflow.cases.controller;

import com.caseflow.cases.dto.*;
import com.caseflow.cases.entity.Case;
import com.caseflow.cases.entity.Document;
import com.caseflow.cases.service.CaseService;
import com.caseflow.cases.util.FileStorageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Case Filing & Documentation", description = "File cases, upload documents, verify/reject documents")
public class CaseController {
    private final CaseService caseService;

    @PostMapping("/file") @Operation(summary = "File a new case")
    public ResponseEntity<CaseResponse> fileCase(
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @Valid @RequestBody CaseRequest request) {
        if (userRole == null || userRole.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        if ("ADMIN".equalsIgnoreCase(userRole) || "ROLE_ADMIN".equalsIgnoreCase(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN cannot file cases.");
        }
        return ResponseEntity.ok(caseService.fileCase(request));
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document to a case")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam(name = "caseId") Long caseId,
            @RequestParam(name = "title") String title,
            @RequestParam(name = "type") Document.DocumentType type,
            @RequestParam(name = "uploadedBy") String uploadedBy,
            @RequestParam(name = "uri", required = false) String uri,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        DocumentRequest request = new DocumentRequest();
        request.setCaseId(caseId);
        request.setTitle(title);
        request.setType(type);
        request.setUploadedBy(uploadedBy);
        request.setUri(uri);

        if (file != null && !file.isEmpty()) {
            log.info("Uploading file: {} for Case: {}", file.getOriginalFilename(), caseId);

            FileStorageUtil.validateFile(file);
            String fileLocalPath = FileStorageUtil.saveFile(file);
            log.info("File saved to server: {}", fileLocalPath);

            String contentType = FileStorageUtil.getMimeType(file.getOriginalFilename());
            request.setFileLocalPath(fileLocalPath);
            request.setOriginalFileName(file.getOriginalFilename());
            request.setContentType(contentType);
            request.setUri(uri != null ? uri : "/uploaded/" + file.getOriginalFilename());
        }

        return ResponseEntity.ok(caseService.uploadDocument(request));
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "Download document file - Returns file from server storage. Only ADMIN and CLERK can download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable("documentId") Long documentId,
            @RequestParam(name = "role", required = false) String role) {
        log.info("Downloading document: {}", documentId);

        boolean hasPermission = "ROLE_ADMIN".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role)
                || "ROLE_CLERK".equalsIgnoreCase(role)
                || "CLERK".equalsIgnoreCase(role);

        if (!hasPermission) {
            log.warn("Access denied: Attempted to download document {} without ADMIN or CLERK role", documentId);
            return ResponseEntity.status(403).build();
        }

        DocumentResponse document = caseService.getDocumentById(documentId);
        byte[] fileContent = caseService.downloadDocument(documentId);

        String filename = document.getOriginalFileName() != null ? document.getOriginalFileName() : "document";
        String contentType = document.getContentType() != null ? document.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(fileContent);
    }

    @PatchMapping("/documents/{documentId}/verify") @Operation(summary = "Verify or reject a document")
    public ResponseEntity<DocumentResponse> verifyDocument(@PathVariable Long documentId,
            @Valid @RequestBody VerificationRequest request) {
        return ResponseEntity.ok(caseService.verifyDocument(documentId, request));
    }

    @PatchMapping("/{caseId}/status")
    @Operation(summary = "Update case status")
    public ResponseEntity<CaseResponse> updateCaseStatus(
            @PathVariable("caseId") Long caseId,
            @RequestParam(name = "newStatus") Case.CaseStatus newStatus,
            @RequestParam(name = "updatedBy") String updatedBy) {
        return ResponseEntity.ok(caseService.updateCaseStatus(caseId, newStatus, updatedBy));
    }

    @GetMapping("/{caseId}") public ResponseEntity<CaseResponse> getCaseById(
            @PathVariable Long caseId,
            @RequestHeader(value = "X-Auth-User-Role",  required = false) String userRole,
            @RequestHeader(value = "X-Auth-User-Id",    required = false) String userId,
            @RequestHeader(value = "X-Auth-User-Email", required = false) String userEmail) {
        CaseResponse c = caseService.getCaseById(caseId);
        // LITIGANT may only open their own cases
        if ("LITIGANT".equalsIgnoreCase(userRole)) {
            boolean isOwner = (userId != null && userId.equals(c.getLitigantId()))
                           || (userEmail != null && userEmail.equals(c.getLitigantId()));
            if (!isOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own cases.");
            }
        }
        return ResponseEntity.ok(c);
    }

    @GetMapping public ResponseEntity<List<CaseResponse>> getAllCases(
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        if ("LITIGANT".equalsIgnoreCase(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "LITIGANTs cannot list all cases. Use /api/cases/litigant/{id} instead.");
        }
        return ResponseEntity.ok(caseService.getAllCases());
    }

    @GetMapping("/litigant/{litigantId}") public ResponseEntity<List<CaseResponse>> getCasesByLitigant(
            @PathVariable String litigantId,
            @RequestHeader(value = "X-Auth-User-Role",  required = false) String userRole,
            @RequestHeader(value = "X-Auth-User-Id",    required = false) String userId,
            @RequestHeader(value = "X-Auth-User-Email", required = false) String userEmail) {
        // LITIGANT can only fetch their own cases
        if ("LITIGANT".equalsIgnoreCase(userRole)) {
            boolean isOwner = litigantId.equals(userId) || litigantId.equals(userEmail);
            if (!isOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own cases.");
            }
        }
        return ResponseEntity.ok(caseService.getCasesByLitigant(litigantId));
    }

    @GetMapping("/lawyer/{lawyerId}") public ResponseEntity<List<CaseResponse>> getCasesByLawyer(@PathVariable String lawyerId) {
        return ResponseEntity.ok(caseService.getCasesByLawyer(lawyerId));
    }

    @GetMapping("/status/{status}") public ResponseEntity<List<CaseResponse>> getCasesByStatus(
            @PathVariable Case.CaseStatus status,
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        if ("LITIGANT".equalsIgnoreCase(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "LITIGANTs cannot filter all cases by status. Use /api/cases/litigant/{id} and filter client-side.");
        }
        return ResponseEntity.ok(caseService.getCasesByStatus(status));
    }
    @GetMapping("/{caseId}/documents") public ResponseEntity<List<DocumentResponse>> getDocumentsByCaseId(@PathVariable Long caseId) {
        return ResponseEntity.ok(caseService.getDocumentsByCaseId(caseId));
    }
    @GetMapping("/documents/pending") public ResponseEntity<List<DocumentResponse>> getPendingDocuments() {
        return ResponseEntity.ok(caseService.getPendingDocuments());
    }
    @GetMapping("/documents/{documentId}") public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long documentId) {
        return ResponseEntity.ok(caseService.getDocumentById(documentId));
    }

    // Internal endpoint for other services
    @PatchMapping("/internal/{caseId}/type")
    public ResponseEntity<Void> setCaseType(@PathVariable Long caseId, @RequestParam String caseType) {
        caseService.setCaseType(caseId, caseType);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/internal/{caseId}/status")
    public ResponseEntity<CaseResponse> updateCaseStatusInternal(
            @PathVariable("caseId") Long caseId,
            @RequestParam(name = "newStatus") Case.CaseStatus newStatus,
            @RequestParam(name = "updatedBy", required = false, defaultValue = "0") String updatedBy) {
        return ResponseEntity.ok(caseService.updateCaseStatus(caseId, newStatus, updatedBy));
    }
}
