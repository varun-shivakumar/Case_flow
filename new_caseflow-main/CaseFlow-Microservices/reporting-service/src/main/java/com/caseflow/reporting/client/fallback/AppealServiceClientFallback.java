package com.caseflow.reporting.client.fallback;

import com.caseflow.reporting.client.AppealServiceClient;
import com.caseflow.reporting.client.dto.AppealDto;
import com.caseflow.reporting.client.dto.PageResponse;
import com.caseflow.reporting.client.dto.ReviewDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class AppealServiceClientFallback implements AppealServiceClient {

    @Override
    public PageResponse<AppealDto> getAppealsPaginated(int page, int size) {
        log.warn("appeal-service unavailable — returning empty paginated appeals");
        PageResponse<AppealDto> empty = new PageResponse<>();
        empty.setContent(Collections.emptyList());
        empty.setEmpty(true);
        empty.setTotalElements(0);
        empty.setTotalPages(0);
        return empty;
    }

    @Override
    public List<AppealDto> getAppealsByCase(Long caseId) {
        log.warn("appeal-service unavailable — returning empty appeals for case #{}", caseId);
        return Collections.emptyList();
    }

    @Override
    public List<AppealDto> getAppealsByUser(String userId) {
        log.warn("appeal-service unavailable — returning empty appeals for user {}", userId);
        return Collections.emptyList();
    }

    @Override
    public List<AppealDto> getAppealsByStatus(String status) {
        log.warn("appeal-service unavailable — returning empty list for status {}", status);
        return Collections.emptyList();
    }

    @Override
    public List<ReviewDto> getReviewsByCase(Long caseId) {
        log.warn("appeal-service unavailable — returning empty reviews for case #{}", caseId);
        return Collections.emptyList();
    }

    @Override
    public List<ReviewDto> getReviewsByJudge(String judgeId) {
        log.warn("appeal-service unavailable — returning empty reviews for judge {}", judgeId);
        return Collections.emptyList();
    }
}
