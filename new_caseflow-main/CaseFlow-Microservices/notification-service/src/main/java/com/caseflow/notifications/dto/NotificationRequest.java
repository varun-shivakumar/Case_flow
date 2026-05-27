package com.caseflow.notifications.dto;

import com.caseflow.notifications.enums.NotificationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    private Long caseId;

    @NotBlank(message = "Message cannot be blank")
    private String message;

    @NotNull(message = "Category is required")
    private NotificationCategory category;
}
