package com.caseflow.appeals.service;

import com.caseflow.appeals.dto.response.AppealResponse;
import com.caseflow.appeals.dto.response.ReviewResponse;
import com.caseflow.appeals.repository.AppealRepository;
import com.caseflow.appeals.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Paginated, read-only query service.
 * Returns DTOs (never JPA entities) to avoid leaking the persistence schema.
 */
@Service
@RequiredArgsConstructor
public class CachedAppealService {

    private final AppealRepository appealRepository;
    private final ReviewRepository reviewRepository;
    private final AppealService    appealService;
    private final ReviewService    reviewService;

    @Transactional(readOnly = true)
    public Page<AppealResponse> getAllAppealsPaginated(Pageable pageable) {
        return appealRepository.findAll(pageable).map(appealService::toAppealResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviewsPaginated(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(reviewService::toReviewResponse);
    }
}
