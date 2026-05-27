package com.caseflow.hearing.client;

import com.caseflow.hearing.client.fallback.IamServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "iam-service", fallback = IamServiceFallback.class)
public interface IamServiceClient {
    @GetMapping("/api/users/exists/{id}")
    Boolean existsById(@PathVariable("id") String id);
    @GetMapping("/api/users/{id}/role")
    String getUserRole(@PathVariable("id") String id);
}
