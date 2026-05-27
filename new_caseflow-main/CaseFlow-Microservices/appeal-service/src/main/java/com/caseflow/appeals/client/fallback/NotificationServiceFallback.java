package com.caseflow.appeals.client.fallback;

import com.caseflow.appeals.client.NotificationServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class NotificationServiceFallback implements FallbackFactory<NotificationServiceClient> {

    @Override
    public NotificationServiceClient create(Throwable cause) {
        return request -> log.warn(
            "CIRCUIT BREAKER: notification-service unavailable — notification skipped: {} | cause: {}",
            request.get("message"),
            cause != null ? cause.getMessage() : "Unknown error");
    }
}
