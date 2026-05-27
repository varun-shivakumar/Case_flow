package com.caseflow.iam.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank @Email private String email;
    @NotBlank private String oldPassword;
    @NotBlank @Size(min = 6, max = 100) private String newPassword;
}
