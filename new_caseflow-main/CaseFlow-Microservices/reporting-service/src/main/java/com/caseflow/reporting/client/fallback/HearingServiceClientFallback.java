package com.caseflow.reporting.client.fallback;

import com.caseflow.reporting.client.HearingServiceClient;
import com.caseflow.reporting.client.dto.HearingDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class HearingServiceClientFallback implements HearingServiceClient {

    @Override
    public List<HearingDto> getAllHearings() {
        log.warn("hearing-service unavailable — returning empty hearing list for report");
        return Collections.emptyList();
    }

    @Override
    public List<HearingDto> getHearingsByCase(Long caseId) {
        log.warn("hearing-service unavailable — returning empty list for case #{}", caseId);
        return Collections.emptyList();
    }

    @Override
    public List<HearingDto> getHearingsByJudge(Long judgeId) {
        log.warn("hearing-service unavailable — returning empty list for judge #{}", judgeId);
        return Collections.emptyList();
    }
}
