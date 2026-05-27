package com.caseflow.appeals.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Supporting document attached to an appeal — petitions, evidence, briefs,
 * affidavits, prior judgments, etc. Files live on disk under
 * caseflow-uploads/appeals/; this entity stores only the metadata.
 */
@Entity
@Table(name = "appeal_documents", indexes = {
    @Index(name = "idx_appeal_doc_appeal_id", columnList = "appeal_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "appeal_id", nullable = false)
    private Long appealId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentType type;

    @Column(name = "uploaded_by", nullable = false, length = 50)
    private String uploadedBy;

    @Column(name = "uploaded_date", nullable = false)
    private LocalDateTime uploadedDate;

    @Column(name = "file_local_path", length = 500)
    private String fileLocalPath;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    public enum DocumentType {
        PETITION, EVIDENCE, BRIEF, AFFIDAVIT, PRIOR_JUDGMENT, NOTICE, OTHER
    }
}
