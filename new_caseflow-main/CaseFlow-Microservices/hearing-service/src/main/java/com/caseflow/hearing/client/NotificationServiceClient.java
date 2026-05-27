package com.caseflow.hearing.client;

import com.caseflow.hearing.client.fallback.NotificationServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "notification-service", fallback = NotificationServiceFallback.class)
public interface NotificationServiceClient {
    @PostMapping("/api/notifications/internal")
    void sendNotification(@RequestBody Map<String, Object> request);
}
