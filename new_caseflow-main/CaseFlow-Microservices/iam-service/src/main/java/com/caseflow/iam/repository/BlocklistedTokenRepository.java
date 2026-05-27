package com.caseflow.iam.repository;

import com.caseflow.iam.entity.BlocklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BlocklistedTokenRepository extends JpaRepository<BlocklistedToken, Long> {
    Optional<BlocklistedToken> findByToken(String token);
    boolean existsByToken(String token);
    void deleteByExpiryDateBefore(LocalDateTime dateTime);
}
