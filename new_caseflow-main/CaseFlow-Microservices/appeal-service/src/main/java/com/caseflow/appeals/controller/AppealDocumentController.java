package com.caseflow.appeals.controller;

import com.caseflow.appeals.dto.response.AppealDocumentResponse;
import com.caseflow.appeals.entity.AppealDocument.DocumentType;
import com.caseflow.appeals.service.AppealDocumentService;
import com.caseflow.appeals.service.AppealDocumentService.DownloadPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Document endpoints for appeals — multipart upload, list, metadata, download, delete.
 *
 * Path conventions follow case-service's document API:
 *  - Per-appeal scoped: /api/appeals/{id}/documents
 *  - Document-id-scoped: /api/appeals/documents/{docId}
 */
@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
@Tag(name = "Appeal Documents", description = "Attach, list, download, and delete supporting documents for an appeal")
public class AppealDocumentController {

    private final AppealDocumentService documentService;
    private final RoleGuard             roleGuard;

    @Operation(
        summary     = "Upload a document to an appeal",
        description = "The filer (while SUBMITTED) or an ADMIN can attach supporting documents " +
                      "(petition, evidence, brief, affidavit, prior judgment, notice, other). " +
                      "Max file size 10MB; allowed types: pdf, doc(x), xls(x), ppt(x), txt, jpg, jpeg, png, gif, zip, rar."
    )
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AppealDocumentResponse> upload(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable("id") Long appealId,
            @RequestParam("title") String title,
            @RequestParam("type") DocumentType type,
            @RequestPart("file") MultipartFile file) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "ADMIN");
        roleGuard.requireUserId(currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(documentService.upload(appealId, title, type, file, currentUserId, userRole));
    }

    @Operation(summary = "List documents attached to an appeal")
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<AppealDocumentResponse>> list(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable("id") Long appealId) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "JUDGE", "CLERK", "ADMIN");
        roleGuard.requireUserId(currentUserId);
        return ResponseEntity.ok(documentService.listForAppeal(appealId, currentUserId, userRole));
    }

    @Operation(summary = "Get document metadata by id")
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<AppealDocumentResponse> getById(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long documentId) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "JUDGE", "CLERK", "ADMIN");
        roleGuard.requireUserId(currentUserId);
        return ResponseEntity.ok(documentService.getById(documentId, currentUserId, userRole));
    }

    @Operation(summary = "Download the underlying file for a document")
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> download(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long documentId) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "JUDGE", "CLERK", "ADMIN");
        roleGuard.requireUserId(currentUserId);
        DownloadPayload payload = documentService.download(documentId, currentUserId, userRole);

        String filename = payload.filename() != null ? payload.filename() : "document";
        String contentType = payload.contentType() != null
            ? payload.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(payload.bytes());
    }

    @Operation(
        summary     = "Delete a document from an appeal",
        description = "The filer can delete only while the appeal is SUBMITTED. ADMIN can delete any time."
    )
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Id",   required = false) String currentUserId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole,
            @PathVariable Long documentId) {

        roleGuard.requireAnyRole(userRole, "LITIGANT", "LAWYER", "ADMIN");
        roleGuard.requireUserId(currentUserId);
        documentService.delete(documentId, currentUserId, userRole);
        return ResponseEntity.noContent().build();
    }
}
