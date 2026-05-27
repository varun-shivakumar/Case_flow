package com.caseflow.cases.client.fallback;

import com.caseflow.cases.client.IamServiceClient;
import com.caseflow.cases.client.dto.UserRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
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

    @Override
    public List<UserRef> getUsersByRole(String role) {
        log.warn("CIRCUIT BREAKER: iam-service unavailable — getUsersByRole({}) returning empty list — notifications to this role will be skipped", role);
        return Collections.emptyList();
    }
}