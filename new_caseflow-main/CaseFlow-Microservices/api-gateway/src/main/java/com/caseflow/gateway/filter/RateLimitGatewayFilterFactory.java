package com.caseflow.gateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory token-bucket rate limiter.
 *
 * Configured per-route in application.yml:
 *
 *     filters:
 *       - RateLimit=10,60,POST    # 10 requests / 60 seconds, only POST is rate-limited
 *
 * Args (comma-separated):
 *  1. capacity      — bucket size (max tokens)
 *  2. refillSeconds — interval over which the bucket refills to full capacity
 *  3. methods       — optional CSV of HTTP methods to enforce (e.g. POST or POST|PATCH).
 *                     Use "ALL" or omit to enforce on every method.
 *
 * Buckets are keyed by the X-Auth-User-Id header injected by AuthenticationFilter,
 * falling back to client IP when the request is unauthenticated. State is held in
 * memory — adequate for a single gateway instance; multi-instance deployments
 * should switch to a shared store (e.g. Redis).
 */
@Component
@Slf4j
public class RateLimitGatewayFilterFactory
        extends AbstractGatewayFilterFactory<RateLimitGatewayFilterFactory.Config> {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("capacity", "refillSeconds", "methods");
    }

    @Override
    public GatewayFilter apply(Config config) {
        if (config.getCapacity() <= 0 || config.getRefillSeconds() <= 0) {
            throw new IllegalArgumentException(
                "RateLimit requires positive capacity and refillSeconds");
        }
        Set<HttpMethod> enforcedMethods = parseMethods(config.getMethods());
        long refillNanos = config.getRefillSeconds() * 1_000_000_000L;

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!enforcedMethods.isEmpty() && !enforcedMethods.contains(request.getMethod())) {
                return chain.filter(exchange);
            }

            String key = resolveKey(request);
            TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(config.getCapacity(), refillNanos));

            if (bucket.tryConsume()) {
                return chain.filter(exchange);
            }
            log.warn("Rate limit exceeded for key=[{}] on {} {}",
                key, request.getMethod(), request.getURI().getPath());
            return reject(exchange.getResponse(), config);
        };
    }

    private static Set<HttpMethod> parseMethods(String methods) {
        if (methods == null || methods.isBlank() || "ALL".equalsIgnoreCase(methods.trim())) {
            return Set.of();
        }
        // accept either ',' or '|' as separator inside the SPEL-style shortcut
        String[] tokens = methods.split("[|,]");
        return Arrays.stream(tokens)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> HttpMethod.valueOf(s.toUpperCase()))
            .collect(Collectors.toUnmodifiableSet());
    }

    private static String resolveKey(ServerHttpRequest request) {
        String userId = request.getHeaders().getFirst("X-Auth-User-Id");
        if (userId != null && !userId.isBlank()) return "user:" + userId;
        var remote = request.getRemoteAddress();
        return "ip:" + (remote != null && remote.getAddress() != null
            ? remote.getAddress().getHostAddress() : "unknown");
    }

    private static Mono<Void> reject(ServerHttpResponse response, Config config) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("Retry-After", String.valueOf(config.getRefillSeconds()));
        String body = "{\"status\":429,\"message\":\"Rate limit exceeded — try again in "
            + config.getRefillSeconds() + " seconds.\"}";
        DataBuffer buf = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buf));
    }

    @Data
    public static class Config {
        private int    capacity;
        private int    refillSeconds;
        private String methods;
    }

    /**
     * Simple non-blocking token bucket.
     * Refills proportionally on each consume attempt — no background thread.
     */
    static final class TokenBucket {
        private final int  capacity;
        private final long refillNanos;
        private double     tokens;
        private long       lastRefillNanos;

        TokenBucket(int capacity, long refillNanos) {
            this.capacity        = capacity;
            this.refillNanos     = refillNanos;
            this.tokens          = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed > 0) {
                double refill = (double) elapsed / refillNanos * capacity;
                tokens = Math.min(capacity, tokens + refill);
                lastRefillNanos = now;
            }
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
