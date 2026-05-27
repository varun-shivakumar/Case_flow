package com.caseflow.appeals.service;

import com.caseflow.appeals.dto.response.AppealDocumentResponse;
import com.caseflow.appeals.entity.Appeal;
import com.caseflow.appeals.entity.Appeal.AppealStatus;
import com.caseflow.appeals.entity.AppealAudit.Action;
import com.caseflow.appeals.entity.AppealDocument;
import com.caseflow.appeals.entity.AppealDocument.DocumentType;
import com.caseflow.appeals.exception.InvalidOperationException;
import com.caseflow.appeals.exception.ResourceNotFoundException;
import com.caseflow.appeals.repository.AppealDocumentRepository;
import com.caseflow.appeals.repository.AppealRepository;
import com.caseflow.appeals.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages supporting documents attached to an appeal.
 *
 * Permission model:
 *  - Upload  : the appeal filer (while SUBMITTED) or ADMIN (any time except DECIDED/CANCELLED).
 *  - List/get/download : filer, ADMIN, CLERK, JUDGE.
 *  - Delete  : the filer (while SUBMITTED) or ADMIN.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppealDocumentService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CLERK = "CLERK";
    private static final String ROLE_JUDGE = "JUDGE";

    private final AppealRepository         appealRepository;
    private final AppealDocumentRepository documentRepository;
    private final AppealAuditService       audit;

    @Transactional
    public AppealDocumentResponse upload(Long appealId, String title, DocumentType type,
                                         MultipartFile file,
                                         String currentUserId, String userRole) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("A file is required to upload a document.");
        }
        if (title == null || title.isBlank()) {
            throw new InvalidOperationException("Document title is required.");
        }
        if (type == null) {
            throw new InvalidOperationException("Document type is required.");
        }

        Appeal appeal = findAppealOrThrow(appealId);
        AppealStatus status = appeal.getStatus();
        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(userRole);
        boolean isFiler = currentUserId != null && currentUserId.equals(appeal.getFiledByUserId());

        if (status == AppealStatus.DECIDED || status == AppealStatus.CANCELLED) {
            throw new InvalidOperationException(
                "Cannot upload documents to a " + status + " appeal.");
        }
        if (!isAdmin) {
            if (!isFiler) {
                throw new InvalidOperationException(
                    "Access denied: only the appeal filer or an ADMIN can attach documents.");
            }
            if (status != AppealStatus.SUBMITTED) {
                throw new InvalidOperationException(
                    "The filer can only attach documents while the appeal is SUBMITTED. " +
                    "Current state: " + status + ". Contact an administrator.");
            }
        }

        String localPath;
        try {
            localPath = FileStorageUtil.saveFile(file);
        } catch (IOException e) {
            throw new InvalidOperationException("Failed to store uploaded file: " + e.getMessage());
        }

        AppealDocument doc = AppealDocument.builder()
            .appealId(appealId)
            .title(title.trim())
            .type(type)
            .uploadedBy(currentUserId)
            .uploadedDate(LocalDateTime.now())
            .fileLocalPath(localPath)
            .originalFileName(file.getOriginalFilename())
            .contentType(FileStorageUtil.getMimeType(file.getOriginalFilename()))
            .build();
        doc = documentRepository.save(doc);
        doc.setFileUrl(FileStorageUtil.generateFileUrl(doc.getDocumentId()));
        doc = documentRepository.save(doc);

        log.info("Document #{} uploaded for appeal #{} by [{}]",
            doc.getDocumentId(), appealId, currentUserId);

        audit.record(appealId, Action.DOCUMENT_UPLOADED,
            currentUserId, userRole,
            status, status,
            "documentId=" + doc.getDocumentId() + ";title=" + doc.getTitle()
                + ";type=" + type + ";originalFileName=" + doc.getOriginalFileName());

        return toResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<AppealDocumentResponse> listForAppeal(Long appealId,
                                                      String currentUserId, String userRole) {
        Appeal appeal = findAppealOrThrow(appealId);
        ensureCanRead(appeal, currentUserId, userRole);
        return documentRepository.findByAppealIdOrderByUploadedDateDesc(appealId)
            .stream().map(AppealDocumentService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AppealDocumentResponse getById(Long documentId,
                                          String currentUserId, String userRole) {
        AppealDocument doc = findDocOrThrow(documentId);
        ensureCanRead(findAppealOrThrow(doc.getAppealId()), currentUserId, userRole);
        return toResponse(doc);
    }

    @Transactional(readOnly = true)
    public DownloadPayload download(Long documentId,
                                    String currentUserId, String userRole) {
        AppealDocument doc = findDocOrThrow(documentId);
        ensureCanRead(findAppealOrThrow(doc.getAppealId()), currentUserId, userRole);
        if (doc.getFileLocalPath() == null || doc.getFileLocalPath().isBlank()) {
            throw new InvalidOperationException("No file is associated with document #" + documentId);
        }
        try {
            byte[] bytes = FileStorageUtil.readFile(doc.getFileLocalPath());
            return new DownloadPayload(bytes, doc.getOriginalFileName(), doc.getContentType());
        } catch (IOException e) {
            log.error("Error reading file for document #{}: {}", documentId, e.getMessage());
            throw new InvalidOperationException("Error retrieving file: " + e.getMessage());
        }
    }

    @Transactional
    public void delete(Long documentId, String currentUserId, String userRole) {
        AppealDocument doc = findDocOrThrow(documentId);
        Appeal appeal = findAppealOrThrow(doc.getAppealId());

        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(userRole);
        boolean isFiler = currentUserId != null && currentUserId.equals(appeal.getFiledByUserId());
        if (!isAdmin) {
            if (!isFiler) {
                throw new InvalidOperationException(
                    "Access denied: only the appeal filer or an ADMIN can delete an appeal document.");
            }
            if (appeal.getStatus() != AppealStatus.SUBMITTED) {
                throw new InvalidOperationException(
                    "The filer can only delete documents while the appeal is SUBMITTED. " +
                    "Current state: " + appeal.getStatus() + ".");
            }
        }

        documentRepository.delete(doc);
        FileStorageUtil.deleteFile(doc.getFileLocalPath());
        log.info("Document #{} deleted from appeal #{} by [{}]", documentId, doc.getAppealId(), currentUserId);

        audit.record(doc.getAppealId(), Action.DOCUMENT_DELETED,
            currentUserId, userRole,
            appeal.getStatus(), appeal.getStatus(),
            "documentId=" + documentId + ";title=" + doc.getTitle());
    }

    private void ensureCanRead(Appeal appeal, String currentUserId, String userRole) {
        boolean isPrivileged = ROLE_ADMIN.equalsIgnoreCase(userRole)
                            || ROLE_CLERK.equalsIgnoreCase(userRole)
                            || ROLE_JUDGE.equalsIgnoreCase(userRole);
        if (isPrivileged) return;
        if (currentUserId != null && currentUserId.equals(appeal.getFiledByUserId())) return;
        throw new InvalidOperationException(
            "Access denied: only the appeal filer, an assigned judge, a clerk, or an ADMIN " +
            "can view documents for this appeal.");
    }

    private Appeal findAppealOrThrow(Long appealId) {
        return appealRepository.findById(appealId)
            .orElseThrow(() -> new ResourceNotFoundException("Appeal not found: #" + appealId));
    }

    private AppealDocument findDocOrThrow(Long documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appeal document not found: #" + documentId));
    }

    static AppealDocumentResponse toResponse(AppealDocument d) {
        return AppealDocumentResponse.builder()
            .documentId(d.getDocumentId())
            .appealId(d.getAppealId())
            .title(d.getTitle())
            .type(d.getType())
            .uploadedBy(d.getUploadedBy())
            .uploadedDate(d.getUploadedDate())
            .originalFileName(d.getOriginalFileName())
            .contentType(d.getContentType())
            .fileUrl(d.getFileUrl())
            .build();
    }

    public record DownloadPayload(byte[] bytes, String filename, String contentType) {}
}
