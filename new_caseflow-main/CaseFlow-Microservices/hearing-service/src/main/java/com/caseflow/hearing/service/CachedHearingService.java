package com.caseflow.hearing.service;

import com.caseflow.hearing.entity.Hearing;
import com.caseflow.hearing.repository.HearingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedHearingService {
    private final HearingRepository hearingRepository;

    public Page<Hearing> getAllHearingsPaginated(Pageable pageable) {
        return hearingRepository.findAll(pageable);
    }
}
