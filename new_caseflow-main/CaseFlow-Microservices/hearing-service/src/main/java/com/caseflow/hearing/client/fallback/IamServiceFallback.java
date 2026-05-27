package com.caseflow.hearing.client.fallback;

import com.caseflow.hearing.client.IamServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component @Slf4j
public class IamServiceFallback implements IamServiceClient {

    @Override
    public Boolean existsById(String id) {
        log.warn("CIRCUIT BREAKER: iam-service unavailable — existsById({}) returning true as fallback", id);
        return true;
    }

    @Override
    public String getUserRole(String id) {
        log.warn("CIRCUIT BREAKER: iam-service unavailable — getUserRole({}) returning UNKNOWN", id);
        return "UNKNOWN";
    }
}
