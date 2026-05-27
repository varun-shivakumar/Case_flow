package com.caseflow.workflow.repository;
import com.caseflow.workflow.entity.WorkflowStage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkflowStageRepository extends JpaRepository<WorkflowStage, Long> {
    List<WorkflowStage> findByCaseIdOrderBySequenceNumber(Long caseId);
    Optional<WorkflowStage> findByCaseIdAndActiveTrue(Long caseId);
    Optional<WorkflowStage> findByCaseIdAndSequenceNumber(Long caseId, Integer sequenceNumber);
    long countByCaseId(Long caseId);
}
