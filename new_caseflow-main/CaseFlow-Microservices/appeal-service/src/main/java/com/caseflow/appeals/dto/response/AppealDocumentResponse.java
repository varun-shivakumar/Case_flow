package com.caseflow.appeals.dto.response;

import com.caseflow.appeals.entity.AppealDocument.DocumentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppealDocumentResponse {
    private Long          documentId;
    private Long          appealId;
    private String        title;
    private DocumentType  type;
    private String        uploadedBy;
    private LocalDateTime uploadedDate;
    private String        originalFileName;
    private String        contentType;
    private String        fileUrl;
}
