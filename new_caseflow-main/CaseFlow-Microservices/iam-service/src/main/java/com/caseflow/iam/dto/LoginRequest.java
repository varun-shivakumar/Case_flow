package com.caseflow.iam.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") private String email;
    @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") private String password;
}
