package com.caseflow.notifications.service;

import com.caseflow.notifications.dto.NotificationRequest;
import com.caseflow.notifications.dto.NotificationResponse;
import com.caseflow.notifications.enums.NotificationCategory;
import com.caseflow.notifications.enums.NotificationStatus;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest request);

    NotificationResponse createFromInternal(Map<String, Object> payload);

    NotificationResponse getById(Long notificationId);

    NotificationResponse markAsRead(Long notificationId);

    void markAllAsReadForUser(String userId);

    List<NotificationResponse> getByUser(String userId);

    List<NotificationResponse> getByUserAndStatus(String userId, NotificationStatus status);

    List<NotificationResponse> getByUserAndCategory(String userId, NotificationCategory category);

    List<NotificationResponse> getByCase(Long caseId);

    List<NotificationResponse> getByCategory(NotificationCategory category);

    long countUnreadForUser(String userId);
}
