# CaseFlow — Caching & Pagination Patch

## What's Inside
This zip contains ONLY new files to ADD to your existing project.
NO existing files are modified.

## How to Apply
Copy each service folder into your CaseFlow-Microservices directory.
The folder structure mirrors your project exactly — just merge/overlay.

## Per-Service New Files

Each of the 8 business services gets 3 new files:
1. `config/CacheConfig.java` — enables `@EnableCaching` with `ConcurrentMapCacheManager`
2. `config/PaginationConfig.java` — enables `@EnableSpringDataWebSupport`
3. `service/Cached<Module>Service.java` — paginated queries + `@Cacheable` on key lookups
4. `controller/Paginated<Module>Controller.java` — new `/paginated` endpoints

## No POM Changes Needed
`spring-boot-starter-cache` is already transitively included via `spring-boot-starter-data-jpa`.
`Page`/`Pageable` is part of `spring-data-commons` (already in your classpath via JPA starter).
So NO pom.xml changes are required.

## Paginated Endpoint Pattern
All paginated endpoints follow this pattern:
```
GET /api/<module>/paginated?page=0&size=10&sort=id,desc
```
Returns a Spring `Page<T>` JSON with: content, totalElements, totalPages, number, size, etc.

## Cache Behavior
- Uses Spring's ConcurrentMapCacheManager (in-memory, zero config)
- Cached endpoints: getUserById, getCaseById, getReportById, getSLAByStageId
- Cache is per-JVM (each microservice instance has its own cache)
- Cache auto-creates on first access — no cache names need to be pre-registered
