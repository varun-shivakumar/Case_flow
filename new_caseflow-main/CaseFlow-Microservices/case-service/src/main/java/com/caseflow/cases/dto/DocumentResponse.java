package com.caseflow.cases.dto;
import com.caseflow.cases.entity.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentResponse {
    private Long documentId;
    private Long caseId;
    private String title;
    private Document.DocumentType type;
    private String uri;
    private LocalDateTime uploadedDate;
    private Document.VerificationStatus verificationStatus;
    private String uploadedBy;
    /** User-id of the clerk who verified or rejected this document. */
    private String verifiedBy;
    private String rejectionReason;

    private String fileUrl;
    private String originalFileName;
    private String contentType;
}
