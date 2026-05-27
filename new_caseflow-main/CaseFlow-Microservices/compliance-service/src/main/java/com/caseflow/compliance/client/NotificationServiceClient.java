package com.caseflow.compliance.client;

import com.caseflow.compliance.client.dto.NotificationRequest;
import com.caseflow.compliance.client.fallback.NotificationServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", fallback = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

    // Uses the internal service-to-service endpoint (no role headers required)
    @PostMapping("/api/notifications/internal")
    void createNotification(@RequestBody NotificationRequest request);
}
