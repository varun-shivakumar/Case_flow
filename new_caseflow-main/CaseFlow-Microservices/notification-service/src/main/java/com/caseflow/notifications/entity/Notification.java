package com.caseflow.notifications.entity;

import com.caseflow.notifications.enums.NotificationCategory;
import com.caseflow.notifications.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_id",   columnList = "user_id"),
        @Index(name = "idx_notifications_user_status", columnList = "user_id, status"),
        @Index(name = "idx_notifications_case_id",   columnList = "case_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "case_id")
    private Long caseId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();
}
