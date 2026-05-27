package com.caseflow.cases.client;

import com.caseflow.cases.client.dto.UserRef;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "iam-service")
public interface IamServiceClient {
    @GetMapping("/api/users/exists/{id}")
    Boolean existsById(@PathVariable("id") String id);

    @GetMapping("/api/users/{id}/role")
    String getUserRole(@PathVariable("id") String id);

    /** Returns all users with the given role (e.g. CLERK, JUDGE, ADMIN). */
    @GetMapping("/api/users/role/{role}")
    List<UserRef> getUsersByRole(@PathVariable("role") String role);
}
