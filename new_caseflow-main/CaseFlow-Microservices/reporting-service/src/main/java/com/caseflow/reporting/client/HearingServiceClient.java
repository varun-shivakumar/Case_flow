package com.caseflow.reporting.client;

import com.caseflow.reporting.client.dto.HearingDto;
import com.caseflow.reporting.client.fallback.HearingServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "hearing-service", fallback = HearingServiceClientFallback.class)
public interface HearingServiceClient {

    @GetMapping("/api/hearings")
    List<HearingDto> getAllHearings();

    @GetMapping("/api/hearings/case/{caseId}")
    List<HearingDto> getHearingsByCase(@PathVariable Long caseId);

    @GetMapping("/api/hearings/judge/{judgeId}")
    List<HearingDto> getHearingsByJudge(@PathVariable Long judgeId);
}
