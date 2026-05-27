package com.caseflow.compliance.client.fallback;

import com.caseflow.compliance.client.NotificationServiceClient;
import com.caseflow.compliance.client.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public void createNotification(NotificationRequest request) {
        log.warn("notification-service unavailable — notification dropped: {}", request.getMessage());
    }
}
