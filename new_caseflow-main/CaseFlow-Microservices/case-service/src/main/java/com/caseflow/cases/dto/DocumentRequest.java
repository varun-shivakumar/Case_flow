package com.caseflow.cases.dto;
import com.caseflow.cases.entity.Document;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DocumentRequest {
    @NotNull(message = "Case ID is required")
    private Long caseId;
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 255) private String title;
    @NotNull(message = "Document type is required")
    private Document.DocumentType type;
    private String uri;
    @NotNull(message = "Uploaded By is required")
    private String uploadedBy;

    private String fileLocalPath;
    private String originalFileName;
    private String contentType;
}
