package com.caseflow.notifications.dto;

import com.caseflow.notifications.enums.NotificationCategory;
import com.caseflow.notifications.enums.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long notificationId;
    private String userId;
    private Long caseId;
    private String message;
    private NotificationCategory category;
    private NotificationStatus status;
    private LocalDateTime createdDate;
}
