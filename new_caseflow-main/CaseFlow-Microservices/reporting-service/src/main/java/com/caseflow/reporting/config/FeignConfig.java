package com.caseflow.reporting.config;

import feign.Logger;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forwards X-Auth-User-Id and X-Auth-User-Role headers from the inbound request
 * to outbound Feign calls so downstream services can enforce their own role guards.
 *
 * Without this interceptor, calls from reporting-service to appeal-service or
 * compliance-service paginated endpoints would fail with 401/403 because they
 * require ADMIN/CLERK roles.
 */
@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            HttpServletRequest req = attrs.getRequest();
            String userId    = req.getHeader("X-Auth-User-Id");
            String userRole  = req.getHeader("X-Auth-User-Role");
            String userEmail = req.getHeader("X-Auth-User-Email");
            String authHdr   = req.getHeader("Authorization");
            if (userId    != null && !userId.isBlank())    template.header("X-Auth-User-Id",    userId);
            if (userRole  != null && !userRole.isBlank())  template.header("X-Auth-User-Role",  userRole);
            if (userEmail != null && !userEmail.isBlank()) template.header("X-Auth-User-Email", userEmail);
            // Also forward the bearer JWT — needed when reporting-service hits
            // any iam-service endpoint protected by Spring Security @PreAuthorize.
            if (authHdr   != null && !authHdr.isBlank())   template.header("Authorization",     authHdr);
        };
    }
}
