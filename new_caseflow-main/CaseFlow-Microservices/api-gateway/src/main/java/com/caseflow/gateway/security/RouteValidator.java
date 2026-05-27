package com.caseflow.gateway.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Defines which API paths are open (no JWT required) vs secured.
 */
@Component
public class RouteValidator {

    /**
     * Public endpoints that don't require authentication.
     */
    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/users/exists",
            "/api/users/validate",
            "/eureka",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/api-docs",
            "/actuator",
            // Per-service Swagger doc proxy paths (routed through gateway with StripPrefix=1)
            "/iam/api-docs",
            "/case/api-docs",
            "/hearing/api-docs",
            "/workflow/api-docs",
            "/appeal/api-docs",
            "/compliance/api-docs",
            "/notification/api-docs",
            "/reporting/api-docs"
    );

    /**
     * Predicate that returns true if the request path is secured (not open).
     */
    public Predicate<String> isSecured =
            path -> OPEN_API_ENDPOINTS.stream().noneMatch(path::startsWith);
}
