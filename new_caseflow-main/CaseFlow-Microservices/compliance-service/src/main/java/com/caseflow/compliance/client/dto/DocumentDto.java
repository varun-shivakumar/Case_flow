package com.caseflow.compliance.client.dto;

import lombok.Data;

@Data
public class DocumentDto {
    private Long documentId;
    private String title;
    private String verificationStatus;
}
