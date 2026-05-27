package com.caseflow.iam.dto;
import com.caseflow.iam.entity.User;
import lombok.Data;

@Data
public class UserResponse {
    private String userId;
    private String name;
    private User.Role role;
    private String email;
    private String phone;
    private User.Status status;
}