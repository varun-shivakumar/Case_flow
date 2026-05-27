package com.caseflow.cases.service;

import com.caseflow.cases.dto.CaseResponse;
import com.caseflow.cases.dto.DocumentResponse;
import com.caseflow.cases.entity.Case;
import com.caseflow.cases.entity.Document;
import com.caseflow.cases.exception.ResourceNotFoundException;
import com.caseflow.cases.repository.CaseRepository;
import com.caseflow.cases.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedCaseService {

    private final CaseRepository caseRepository;
    private final DocumentRepository documentRepository;

    @Cacheable(value = "cases", key = "#caseId")
    public CaseResponse getCachedCaseById(Long caseId) {
        Case c = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        CaseResponse res = new CaseResponse();
        res.setCaseId(c.getCaseId()); res.setTitle(c.getTitle());
        res.setLitigantId(c.getLitigantId()); res.setLawyerId(c.getLawyerId());
        res.setFiledDate(c.getFiledDate()); res.setStatus(c.getStatus());
        return res;
    }

    public Page<Case> getAllCasesPaginated(Pageable pageable) {
        return caseRepository.findAll(pageable);
    }

    public Page<Document> getAllDocumentsPaginated(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }
}
