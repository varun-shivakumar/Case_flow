package com.caseflow.workflow.client.fallback;

import com.caseflow.workflow.client.NotificationServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component @Slf4j
public class NotificationServiceFallback implements NotificationServiceClient {

    @Override
    public void sendNotification(Map<String, Object> request) {
        log.warn("CIRCUIT BREAKER: notification-service unavailable — notification skipped: {}", request.get("message"));
    }
}
