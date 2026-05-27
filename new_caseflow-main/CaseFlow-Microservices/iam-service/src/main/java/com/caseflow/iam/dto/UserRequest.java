package com.caseflow.iam.dto;
import com.caseflow.iam.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "Name is required") @Size(min = 2, max = 100) private String name;
    @NotNull(message = "Role is required") private User.Role role;
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") private String email;
    @NotBlank(message = "Phone is required") @Pattern(regexp = "^\\d{10}$") private String phone;
    @NotBlank(message = "Password is required") @Size(min = 6, max = 100) private String password;
}
