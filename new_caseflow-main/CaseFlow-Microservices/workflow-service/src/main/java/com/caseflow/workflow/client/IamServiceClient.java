package com.caseflow.workflow.client;

import com.caseflow.workflow.client.dto.UserRef;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "iam-service")
public interface IamServiceClient {
    /** Returns all users with the given role (e.g. ADMIN). */
    @GetMapping("/api/users/role/{role}")
    List<UserRef> getUsersByRole(@PathVariable("role") String role);
}
