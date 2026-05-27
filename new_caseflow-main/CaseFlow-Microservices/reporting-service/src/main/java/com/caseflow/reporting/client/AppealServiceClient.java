package com.caseflow.reporting.client;

import com.caseflow.reporting.client.dto.AppealDto;
import com.caseflow.reporting.client.dto.PageResponse;
import com.caseflow.reporting.client.dto.ReviewDto;
import com.caseflow.reporting.client.fallback.AppealServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "appeal-service", fallback = AppealServiceClientFallback.class)
public interface AppealServiceClient {

    @GetMapping("/api/appeals/paginated")
    PageResponse<AppealDto> getAppealsPaginated(
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/api/appeals/case/{caseId}")
    List<AppealDto> getAppealsByCase(@PathVariable Long caseId);

    /** Appeals filed by the given user. Used by LAWYER-scope reports to find every
        appeal that lawyer filed, regardless of whether the case is in their case list. */
    @GetMapping("/api/appeals/user/{userId}")
    List<AppealDto> getAppealsByUser(@PathVariable String userId);

    @GetMapping("/api/appeals/status/{status}")
    List<AppealDto> getAppealsByStatus(@PathVariable String status);

    @GetMapping("/api/appeals/reviews/case/{caseId}")
    List<ReviewDto> getReviewsByCase(@PathVariable Long caseId);

    @GetMapping("/api/appeals/reviews/judge/{judgeId}")
    List<ReviewDto> getReviewsByJudge(@PathVariable String judgeId);
}
