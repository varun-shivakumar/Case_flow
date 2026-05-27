package com.caseflow.notifications.service;

import com.caseflow.notifications.entity.Notification;
import com.caseflow.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedNotificationService {

    private final NotificationRepository notificationRepository;

    public Page<Notification> getAllNotificationsPaginated(Pageable pageable) {
        return notificationRepository.findAll(pageable);
    }
}
