package com.caseflow.cases.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring Cache Abstraction for this service.
 * Uses ConcurrentMapCacheManager (in-memory, no external dependency).
 * Cache names are defined here — services use @Cacheable/@CacheEvict on these names.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(); // auto-creates caches on first use
    }
}
