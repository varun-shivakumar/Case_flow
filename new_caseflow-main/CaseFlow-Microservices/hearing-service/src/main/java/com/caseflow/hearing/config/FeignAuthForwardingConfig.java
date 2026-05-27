package com.caseflow.hearing.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Forwards X-Auth-* headers from inbound request to outbound Feign calls. */
@Configuration
public class FeignAuthForwardingConfig {

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
            if (authHdr   != null && !authHdr.isBlank())   template.header("Authorization",     authHdr);
        };
    }
}
