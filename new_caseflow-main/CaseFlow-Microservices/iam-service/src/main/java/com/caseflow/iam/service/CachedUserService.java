package com.caseflow.iam.service;

import com.caseflow.iam.dto.UserResponse;
import com.caseflow.iam.entity.User;
import com.caseflow.iam.exception.ResourceNotFoundException;
import com.caseflow.iam.repository.UserRepository;
import com.caseflow.iam.entity.AuditLog;
import com.caseflow.iam.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Additive service — provides cached lookups and paginated queries.
 * Does NOT replace UserService. Controllers can inject both.
 */
@Service
@RequiredArgsConstructor
public class CachedUserService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Cacheable(value = "users", key = "#id")
    public UserResponse getCachedUserById(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        UserResponse res = new UserResponse();
        res.setUserId(user.getUserId()); res.setName(user.getName());
        res.setRole(user.getRole()); res.setEmail(user.getEmail());
        res.setPhone(user.getPhone()); res.setStatus(user.getStatus());
        return res;
    }

    public Page<User> getAllUsersPaginated(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<AuditLog> getAllAuditLogsPaginated(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
