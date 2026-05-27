package com.caseflow.iam.controller;

import com.caseflow.iam.dto.*;
import com.caseflow.iam.service.PasswordResetService;
import com.caseflow.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication (4.1)",
        description = "Module 4.1 — Public endpoints: register as Litigant, login, change password, forgot/reset password")
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Register as a Litigant")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.registerLitigant(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token. Set rememberMe=true for extended session (7 days).")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PutMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change your own password (new password must differ from old)")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link via email (valid for 10 minutes)")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.forgotPassword(request.getEmail()));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token from the email link (new password must differ from old)")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(
                request.getToken(), request.getNewPassword(), request.getConfirmPassword()));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the currently authenticated user's profile (userId, name, role, email)")
    public ResponseEntity<UserResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(userService.getCurrentUser(email));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout user, invalidate and blocklist the JWT token")
    public ResponseEntity<String> logout(@RequestParam String email, HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        return ResponseEntity.ok(userService.logout(email, token));
    }
}

//resilince4j , zepkin , loadbalances