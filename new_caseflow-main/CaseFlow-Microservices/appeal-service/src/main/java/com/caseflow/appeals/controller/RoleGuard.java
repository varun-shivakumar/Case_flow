package com.caseflow.appeals.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared auth helper injected into both AppealController and ReviewController.
 * JWT validation is done at the API Gateway; the gateway injects
 * X-Auth-User-Id and X-Auth-User-Role headers into every downstream request.
 * This component enforces endpoint-level RBAC without coupling to Spring Security.
 */
@Component
public class RoleGuard {

    public void requireAnyRole(String userRole, String... allowedRoles) {
        if (userRole == null || userRole.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Missing authentication context — please login.");
        }
        for (String role : allowedRoles) {
            if (role.equalsIgnoreCase(userRole)) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Role '" + userRole + "' is not permitted here. Required: "
                + String.join(" or ", allowedRoles));
    }

    public void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Missing authentication context — please login.");
        }
    }
}
