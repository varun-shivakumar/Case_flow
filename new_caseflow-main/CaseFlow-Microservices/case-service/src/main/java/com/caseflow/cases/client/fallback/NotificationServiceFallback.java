package com.caseflow.cases.client.fallback;

import com.caseflow.cases.client.NotificationServiceClient;
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
