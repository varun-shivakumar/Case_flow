package com.caseflow.iam.service;

import com.caseflow.iam.entity.BlocklistedToken;
import com.caseflow.iam.repository.BlocklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final BlocklistedTokenRepository blocklistedTokenRepository;

    /**
     * Blacklist a JWT token (e.g., on logout).
     */
    /**
     * Check if a token is blacklisted.
     */
    public boolean isTokenBlacklisted(String token) {
        return blocklistedTokenRepository.existsByToken(token);
    }

    /**
     * Cleanup expired blacklisted tokens every hour.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        blocklistedTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        log.debug("Cleaned up expired blacklisted tokens");
    }

    public void blocklistToken(String token, LocalDateTime expiryDate) {
        if (!blocklistedTokenRepository.existsByToken(token)) {
            BlocklistedToken blocked = BlocklistedToken.builder()
                    .token(token)
                    .blockedAt(LocalDateTime.now())
                    .expiryDate(expiryDate)
                    .build();
            blocklistedTokenRepository.save(blocked);
            log.info("Token blocklisted successfully");
        }
    }
}
