package com.caseflow.cases.repository;

import com.caseflow.cases.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByCaseId(Long caseId);
    List<Document> findByVerificationStatus(Document.VerificationStatus status);
    long countByCaseId(Long caseId);
    long countByCaseIdAndVerificationStatus(Long caseId, Document.VerificationStatus status);
}
