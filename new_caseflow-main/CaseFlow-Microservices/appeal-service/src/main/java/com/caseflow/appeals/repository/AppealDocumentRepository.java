package com.caseflow.appeals.repository;

import com.caseflow.appeals.entity.AppealDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealDocumentRepository extends JpaRepository<AppealDocument, Long> {
    List<AppealDocument> findByAppealIdOrderByUploadedDateDesc(Long appealId);
    long countByAppealId(Long appealId);
}
