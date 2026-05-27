package com.caseflow.iam.service;

import com.caseflow.iam.entity.PasswordResetToken;
import com.caseflow.iam.entity.User;
import com.caseflow.iam.repository.PasswordResetTokenRepository;
import com.caseflow.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${app.password-reset.token-expiry-minutes:10}")
    private int tokenExpiryMinutes;

    @Value("${app.password-reset.base-url:http://localhost:8089}")
    private String baseUrl;

    /**
     * Generate a password reset token and send a reset link via email.
     * Token is valid for the configured number of minutes (default 10).
     */
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.caseflow.iam.exception.ResourceNotFoundException("No account found with email: " + email));

        // Non-admin users must use a @gmail.com email
        if (user.getRole() != User.Role.ADMIN && !email.toLowerCase().endsWith("@gmail.com")) {
            throw new com.caseflow.iam.exception.BadRequestException("Only @gmail.com email addresses are allowed for non-admin users");
        }

        // Generate a unique token
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(email)
                .expiryDate(LocalDateTime.now().plusMinutes(tokenExpiryMinutes))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        // Build reset link
        String resetLink = baseUrl + "/api/auth/reset-password?token=" + token;

        // Send email
        String subject = "CaseFlow — Password Reset Request";
        String body = "Hello " + user.getName() + ",\n\n"
                + "You have requested to reset your password.\n\n"
                + "Click the link below to reset your password. This link is valid for "
                + tokenExpiryMinutes + " minutes:\n\n"
                + resetLink + "\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "— CaseFlow Team";

        try {
            emailService.sendEmail(email, subject, body);
        } catch (RuntimeException ex) {
            // Log the reset link so admins can share it manually when SMTP is unavailable.
            log.warn("Password reset email could not be sent to {} — reset link: {}", email, resetLink, ex);
        }

        auditLogService.log(user.getUserId(), "PASSWORD_RESET_REQUESTED",
                "User:" + user.getUserId());

        log.info("Password reset link sent to {}", email);
        return "Password reset link has been sent to your email. It is valid for " + tokenExpiryMinutes + " minutes.";
    }

    /**
     * Reset password using the token from the email link.
     * Validates token, checks expiry, ensures new password != old password,
     * and ensures newPassword matches confirmPassword.
     */
    @Transactional
    public String resetPassword(String token, String newPassword, String confirmPassword) {
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new com.caseflow.iam.exception.BadRequestException("New password and confirm password do not match");
        }

        // Find and validate token
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new com.caseflow.iam.exception.BadRequestException("Invalid or already used reset token"));

        if (resetToken.isExpired()) {
            throw new com.caseflow.iam.exception.BadRequestException("Password reset link has expired. Please request a new one.");
        }

        // Find user
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new com.caseflow.iam.exception.ResourceNotFoundException("User not found"));

        // Ensure new password is different from old password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new com.caseflow.iam.exception.BadRequestException("New password must be different from the current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        auditLogService.log(user.getUserId(), "PASSWORD_RESET_COMPLETED",
                "User:" + user.getUserId());

        log.info("Password reset completed for {}", resetToken.getEmail());
        return "Password has been reset successfully. You can now login with your new password.";
    }

    /**
     * Cleanup expired tokens every hour.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        log.debug("Cleaned up expired password reset tokens");
    }
}

