package com.caseflow.iam.service;

import com.caseflow.iam.dto.*;
import com.caseflow.iam.entity.User;
import com.caseflow.iam.exception.*;
import com.caseflow.iam.repository.UserRepository;
import com.caseflow.iam.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final TokenBlacklistService tokenBlocklistService;
    private final EmailService emailService;

    public UserResponse registerLitigant(UserRequest request) {
        request.setRole(User.Role.LITIGANT);
        UserResponse response = register(request);
        try {
            sendLitigantRegistrationEmail(response.getEmail(), response.getName(), response.getUserId());
        } catch (RuntimeException ex) {
            // Keep registration successful even when mail provider is temporarily unavailable.
            log.warn("Litigant registered but confirmation email could not be sent to {}", response.getEmail(), ex);
        }
        return response;
    }

    public UserResponse createUserByAdmin(UserRequest request) {
        UserResponse response = register(request);
        try {
            sendAccountCreationEmail(request.getEmail(), request.getName(), request.getPassword());
        } catch (RuntimeException ex) {
            // Keep user creation successful even when mail provider is temporarily unavailable.
            log.warn("User created but welcome email could not be sent to {}", request.getEmail(), ex);
        }
        return response;
    }

    private void sendAccountCreationEmail(String email, String name, String rawPassword) {
        String subject = "Your CaseFlow Account Has Been Created";
        String body = "Dear " + name + ",\n\n"
                + "Your CaseFlow account has been successfully created by an administrator.\n\n"
                + "Your login credentials are:\n"
                + "  Email:    " + email + "\n"
                + "  Password: " + rawPassword + "\n\n"
                + "IMPORTANT: Please change your password after your first login.\n\n"
                + "Regards,\n"
                + "CaseFlow Admin";
        emailService.sendEmail(email, subject, body);
    }

    private void sendLitigantRegistrationEmail(String email, String name, String userId) {
        String subject = "Your CaseFlow Account Has Been Created";
        String body = "Dear " + name + ",\n\n"
                + "Your CaseFlow account has been successfully created.\n\n"
                + "Your User ID is: " + userId + "\n\n"
                + "Please use this User ID for future support and communication.\n\n"
                + "Regards,\n"
                + "CaseFlow Admin";
        emailService.sendEmail(email, subject, body);
    }

    private UserResponse register(UserRequest request) {
        // Non-admin users must use a @gmail.com email
        if (request.getRole() != User.Role.ADMIN && !request.getEmail().toLowerCase().endsWith("@gmail.com"))
            throw new BadRequestException("Only @gmail.com email addresses are allowed for non-admin users");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        String customId = generateUserId(request.getName(), request.getRole());
        User user = User.builder().userId(customId).name(request.getName()).role(request.getRole())
            .email(request.getEmail()).phone(request.getPhone())
            .password(passwordEncoder.encode(request.getPassword()))
            .status(User.Status.ACTIVE).build();
        User saved = userRepository.save(user);
        auditLogService.log(saved.getUserId(), "USER_REGISTERED", "User:" + saved.getUserId());
        return mapToResponse(saved);
    }

    public LoginResponse login(LoginRequest request) {
        if (request.getPassword() == null || request.getPassword().length() < 6)
            throw new BadRequestException("Password must be at least 6 characters long");
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Non-admin users must have a @gmail.com email
        if (user.getRole() != User.Role.ADMIN && !user.getEmail().toLowerCase().endsWith("@gmail.com"))
            throw new BadRequestException("Only @gmail.com email addresses are allowed for non-admin users");
        if (user.getStatus() == User.Status.INACTIVE)
            throw new InvalidOperationException("User account is inactive");
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());
        auditLogService.log(user.getUserId(), "USER_LOGIN", "User:" + user.getUserId());
        return new LoginResponse(token, user.getRole().name(), user.getName(), user.getEmail(), user.getUserId());
    }

    public String changePassword(ChangePasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword()))
            throw new BadRequestException("Old password is incorrect");
        if (request.getOldPassword().equals(request.getNewPassword()))
            throw new BadRequestException("New password cannot be the same as the old password");
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6)
            throw new BadRequestException("New password must be at least 6 characters long");
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password changed successfully";
    }

    public List<UserResponse> getUsersByRole(String roleName) {
        User.Role role;
        try { role = User.Role.valueOf(roleName.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new BadRequestException("Invalid role: " + roleName); }
        return userRepository.findByRoleAndStatus(role, User.Status.ACTIVE)
            .stream().map(this::mapToResponse).toList();
    }

    public UserResponse getCurrentUser(String email) {
        return mapToResponse(userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public UserResponse getUserById(String id) {
        return mapToResponse(userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id)));
    }

    public UserResponse updateStatus(String id, User.Status status) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setStatus(status);
        return mapToResponse(userRepository.save(user));
    }

    public boolean existsById(String id) { return userRepository.existsById(id); }

    public String adminResetPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6)
            throw new BadRequestException("Password must be at least 6 characters long");
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.log(userId, "ADMIN_RESET_PASSWORD", "User:" + userId);
        return "Password reset successfully for user: " + userId;
    }

    public String getUserRole(String id) {
        return userRepository.findById(id)
            .map(u -> u.getRole().name())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public String deleteUser(String userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userRepository.deleteById(userId);
        return "User deleted successfully";
    }

    private String generateUserId(String name, User.Role role) {
        String prefix = name.replaceAll("[^a-zA-Z]", "").toUpperCase();
        prefix = prefix.length() >= 3 ? prefix.substring(0, 3) : prefix;
        long nextNum = userRepository.countByRole(role) + 1;
        String id = prefix + "_" + role.name() + "_" + nextNum;
        // Ensure uniqueness in case of gaps
        while (userRepository.existsById(id)) {
            nextNum++;
            id = prefix + "_" + role.name() + "_" + nextNum;
        }
        return id;
    }

    private UserResponse mapToResponse(User user) {
        UserResponse res = new UserResponse();
        res.setUserId(user.getUserId()); res.setName(user.getName());
        res.setRole(user.getRole()); res.setEmail(user.getEmail());
        res.setPhone(user.getPhone()); res.setStatus(user.getStatus());
        return res;
    }

    public String logout(String email, String token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Blocklist the token so it cannot be reused
        if (token != null && jwtUtil.isTokenValid(token)) {
            tokenBlocklistService.blocklistToken(token, (LocalDateTime) jwtUtil.extractExpiration(token));
        }

        auditLogService.log(user.getUserId(), "USER_LOGOUT",
                "User:" + user.getUserId());

        return "User logged out successfully. Token has been invalidated.";
    }
}
