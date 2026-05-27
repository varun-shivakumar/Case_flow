package com.caseflow.workflow.service;

import com.caseflow.workflow.entity.WorkflowStage;
import com.caseflow.workflow.entity.SLARecord;
import com.caseflow.workflow.repository.WorkflowStageRepository;
import com.caseflow.workflow.repository.SLARecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedWorkflowService {

    private final WorkflowStageRepository workflowStageRepository;
    private final SLARecordRepository slaRecordRepository;

    public Page<WorkflowStage> getAllStagesPaginated(Pageable pageable) {
        return workflowStageRepository.findAll(pageable);
    }

    public Page<SLARecord> getAllSLARecordsPaginated(Pageable pageable) {
        return slaRecordRepository.findAll(pageable);
    }

    @Cacheable(value = "slaRecords", key = "#stageId")
    public SLARecord getCachedSLAByStageId(Long stageId) {
        return slaRecordRepository.findByStageId(stageId).orElse(null);
    }

    @CacheEvict(value = "slaRecords", key = "#stageId")
    public void evictSLACacheForStage(Long stageId) {
        // evicts the cached SLA entry so next read fetches fresh data from DB
    }
}
