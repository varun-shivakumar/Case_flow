package com.caseflow.cases.service;

import com.caseflow.cases.client.*;
import com.caseflow.cases.dto.*;
import com.caseflow.cases.entity.Case;
import com.caseflow.cases.entity.Document;
import com.caseflow.cases.exception.*;
import com.caseflow.cases.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import jakarta.transaction.Transactional;

import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class CaseService {
    private final CaseRepository caseRepository;
    private final DocumentRepository documentRepository;
    private final IamServiceClient iamClient;
    private final WorkflowServiceClient workflowClient;
    private final NotificationServiceClient notificationClient;

    public CaseResponse fileCase(CaseRequest request) {
        if (!iamClient.existsById(request.getLitigantId()))
            throw new ResourceNotFoundException("Litigant not found: " + request.getLitigantId());
        
        // Normalize lawyerId - treat empty string as null
        String normalizedLawyerId = (request.getLawyerId() != null && !request.getLawyerId().trim().isEmpty()) 
            ? request.getLawyerId().trim() 
            : null;
        
        Case newCase = Case.builder().title(request.getTitle()).litigantId(request.getLitigantId())
            .lawyerId(normalizedLawyerId).filedDate(LocalDateTime.now())
            .status(Case.CaseStatus.FILED).build();
        Case saved = caseRepository.save(newCase);
        
        log.info("Case filed - caseId: {}, litigantId: {}, lawyerId: {}", saved.getCaseId(), saved.getLitigantId(), saved.getLawyerId());
        
        sendNotification(request.getLitigantId(), saved.getCaseId(),
            "Your case '" + saved.getTitle() + "' (Case #" + saved.getCaseId() + ") has been filed.", "CASE");

        if (saved.getLawyerId() != null && !saved.getLawyerId().isBlank()) {
            log.info("Sending notification to lawyer: {}", saved.getLawyerId());
            sendNotification(saved.getLawyerId(), saved.getCaseId(),
                "You have been assigned to Case #" + saved.getCaseId() + " ('" + saved.getTitle() + "').", "CASE");
        } else {
            log.warn("No lawyer assigned to case - lawyerId is null or blank");
        }

        // Per spec: CASE FILED also notifies all CLERKS so they can pick up the case
        // for verification and workflow initiation.
        notifyAllByRole("CLERK", saved.getCaseId(),
            "New case filed: '" + saved.getTitle() + "' (Case #" + saved.getCaseId() + "). Awaiting for workflow initiation.",
            "CASE");

        return mapToCaseResponse(saved);
    }

    @Transactional
    public DocumentResponse uploadDocument(DocumentRequest request) {
        Case existingCase = caseRepository.findById(request.getCaseId())
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + request.getCaseId()));
        if (existingCase.getStatus() == Case.CaseStatus.CLOSED)
            throw new InvalidOperationException("Cannot upload documents to a CLOSED case");
        Document document = Document.builder().caseId(request.getCaseId()).title(request.getTitle())
            .type(request.getType()).uri(request.getUri()).uploadedDate(LocalDateTime.now())
            .verificationStatus(Document.VerificationStatus.PENDING).uploadedBy(request.getUploadedBy())
                .fileLocalPath(request.getFileLocalPath())
                .originalFileName(request.getOriginalFileName())
                .contentType(request.getContentType()).build();
        Document saved = documentRepository.save(document);
        String fileUrl = com.caseflow.cases.util.FileStorageUtil.generateFileUrl(saved.getDocumentId());
        saved.setFileUrl(fileUrl);
        documentRepository.save(saved);


        // Per spec: DOCUMENT UPLOADED notifies CLERKS so they can verify it.
        // We also send a confirmation to the uploader so they know it landed.
        sendNotification(request.getUploadedBy(), request.getCaseId(),
                "Document '" + saved.getTitle() + "' uploaded for Case #" + request.getCaseId() + ". Pending verification.",
                "CASE");
        notifyAllByRole("CLERK", request.getCaseId(),
                "Document '" + saved.getTitle() + "' uploaded for Case #" + request.getCaseId() + " — please verify.",
                "CASE");

        return mapToDocumentResponse(saved);
    }

    public DocumentResponse verifyDocument(Long documentId, VerificationRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (request.getStatus() == Document.VerificationStatus.REJECTED
                && (request.getRejectionReason() == null || request.getRejectionReason().isBlank()))
            throw new InvalidOperationException("Rejection reason is required when rejecting a document");
        document.setVerificationStatus(request.getStatus());
        document.setRejectionReason(request.getStatus() == Document.VerificationStatus.REJECTED
                ? request.getRejectionReason() : null);
        document.setVerifiedBy(request.getClerkId());
        Document saved = documentRepository.save(document);

        String action = request.getStatus() == Document.VerificationStatus.VERIFIED
                ? "DOCUMENT_VERIFIED" : "DOCUMENT_REJECTED";
        log.info("Document {} {} by clerk {}", documentId, action, request.getClerkId());

        Case relatedCase = caseRepository.findById(document.getCaseId()).orElse(null);
        if (relatedCase != null) {
            String msg = request.getStatus() == Document.VerificationStatus.VERIFIED
                    ? "Document '" + saved.getTitle() + "' for Case #" + document.getCaseId() + " has been VERIFIED."
                    : "Document '" + saved.getTitle() + "' for Case #" + document.getCaseId()
                    + " has been REJECTED. Reason: " + request.getRejectionReason();

            sendNotification(relatedCase.getLitigantId(), document.getCaseId(), msg, "CASE");
            if (relatedCase.getLawyerId() != null) {
                sendNotification(relatedCase.getLawyerId(), document.getCaseId(), msg, "CASE");
            }
        }
        checkAndActivateCase(document.getCaseId());
        return mapToDocumentResponse(saved);
    }

    public byte[] downloadDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (document.getFileLocalPath() == null || document.getFileLocalPath().isEmpty()) {
            throw new InvalidOperationException("No file found for document: " + documentId);
        }

        try {
            return com.caseflow.cases.util.FileStorageUtil.readFile(document.getFileLocalPath());
        } catch (Exception e) {
            log.error("Error reading file: {}", e.getMessage());
            throw new InvalidOperationException("Error retrieving file: " + e.getMessage());
        }
    }

    private void checkAndActivateCase(Long caseId) {
        long totalDocs = documentRepository.countByCaseId(caseId);
        long verifiedDocs = documentRepository.countByCaseIdAndVerificationStatus(caseId, Document.VerificationStatus.VERIFIED);
        if (totalDocs > 0 && totalDocs == verifiedDocs) {
            Case existingCase = caseRepository.findById(caseId).orElseThrow(() -> new ResourceNotFoundException("Case not found"));
            if (existingCase.getStatus() == Case.CaseStatus.FILED) {
                existingCase.setStatus(Case.CaseStatus.ACTIVE);
                caseRepository.save(existingCase);
                String msg = "Case #" + caseId + " is now ACTIVE. All documents have been verified.";
                sendNotification(existingCase.getLitigantId(), caseId, msg, "CASE");
                if (existingCase.getLawyerId() != null) {
                    sendNotification(existingCase.getLawyerId(), caseId, msg, "CASE");
                }
                // Per spec: CASE ACTIVE also notifies CLERKS and JUDGES
                // (the case is ready for hearing scheduling and judicial review).
                notifyAllByRole("CLERK", caseId, msg, "CASE");
//                notifyAllByRole("JUDGE", caseId, msg, "CASE");

                log.info("Case {} activated — advancing workflow", caseId);
                workflowClient.advanceWorkflow(caseId);
            }
        }
    }

    public CaseResponse updateCaseStatus(Long caseId, Case.CaseStatus newStatus, String updatedBy) {
        Case existingCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        Case.CaseStatus oldStatus = existingCase.getStatus();
        existingCase.setStatus(newStatus);

        // Stamp closedDate on the CLOSED transition; clear it if the case is reopened
        // (e.g. an appeal moves the case back to ACTIVE / FILED) so deadlines are
        // computed against the most recent close.
        if (newStatus == Case.CaseStatus.CLOSED && oldStatus != Case.CaseStatus.CLOSED) {
            existingCase.setClosedDate(LocalDateTime.now());
        } else if (newStatus != Case.CaseStatus.CLOSED) {
            existingCase.setClosedDate(null);
        }

        Case saved = caseRepository.save(existingCase);

        log.info("Case {} status updated from {} to {} by user {}", caseId, oldStatus, newStatus, updatedBy);

        if (newStatus == Case.CaseStatus.CLOSED) {
            String msg = "Case #" + caseId + " has been CLOSED.";
            sendNotification(saved.getLitigantId(), caseId, msg, "CASE");
            if (saved.getLawyerId() != null) {
                sendNotification(saved.getLawyerId(), caseId, msg, "CASE");
            }
        }
        return mapToCaseResponse(saved);
    }
    public CaseResponse getCaseById(Long caseId) {
        return mapToCaseResponse(caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId)));
    }

    public List<CaseResponse> getAllCases() { return caseRepository.findAll().stream().map(this::mapToCaseResponse).toList(); }
    public List<CaseResponse> getCasesByLitigant(String id) { return caseRepository.findByLitigantId(id).stream().map(this::mapToCaseResponse).toList(); }
    public List<CaseResponse> getCasesByLawyer(String id) { return caseRepository.findByLawyerId(id).stream().map(this::mapToCaseResponse).toList(); }
    public List<CaseResponse> getCasesByStatus(Case.CaseStatus s) { return caseRepository.findByStatus(s).stream().map(this::mapToCaseResponse).toList(); }
    public List<DocumentResponse> getDocumentsByCaseId(Long id) { return documentRepository.findByCaseId(id).stream().map(this::mapToDocumentResponse).toList(); }
    public List<DocumentResponse> getPendingDocuments() { return documentRepository.findByVerificationStatus(Document.VerificationStatus.PENDING).stream().map(this::mapToDocumentResponse).toList(); }
    public DocumentResponse getDocumentById(Long id) { return mapToDocumentResponse(documentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id))); }

    // Internal endpoint for other services
    public void setCaseType(Long caseId, String caseType) {
        Case c = caseRepository.findById(caseId).orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        c.setCaseType(caseType);
        caseRepository.save(c);
    }

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
            log.warn("Notification failed for userId {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Fan out a notification to every active user with the given role.
     * Used to alert all CLERKS / ADMINS on case lifecycle events
     * (per the notification matrix in the system design doc).
     */
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

    private CaseResponse mapToCaseResponse(Case c) {
        CaseResponse res = new CaseResponse();
        res.setCaseId(c.getCaseId()); res.setTitle(c.getTitle());
        res.setLitigantId(c.getLitigantId()); res.setLawyerId(c.getLawyerId());
        res.setFiledDate(c.getFiledDate());
        res.setClosedDate(c.getClosedDate());
        res.setStatus(c.getStatus());
        return res;
    }

    private DocumentResponse mapToDocumentResponse(Document d) {
        DocumentResponse res = new DocumentResponse();
        res.setDocumentId(d.getDocumentId()); res.setCaseId(d.getCaseId());
        res.setTitle(d.getTitle()); res.setType(d.getType());
        res.setUri(d.getUri()); res.setUploadedDate(d.getUploadedDate());
        res.setVerificationStatus(d.getVerificationStatus());
        res.setUploadedBy(d.getUploadedBy()); res.setVerifiedBy(d.getVerifiedBy());
        res.setRejectionReason(d.getRejectionReason());
        res.setFileUrl(d.getFileUrl());
        res.setOriginalFileName(d.getOriginalFileName());
        res.setContentType(d.getContentType());
        return res;
    }
}
