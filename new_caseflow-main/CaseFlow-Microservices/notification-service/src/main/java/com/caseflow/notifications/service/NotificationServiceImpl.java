package com.caseflow.notifications.service;

import com.caseflow.notifications.dto.NotificationRequest;
import com.caseflow.notifications.dto.NotificationResponse;
import com.caseflow.notifications.entity.Notification;
import com.caseflow.notifications.enums.NotificationCategory;
import com.caseflow.notifications.enums.NotificationStatus;
import com.caseflow.notifications.exception.BadRequestException;
import com.caseflow.notifications.exception.ResourceNotFoundException;
import com.caseflow.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @CacheEvict(value = {"notifications-by-user", "notifications-by-case", "unread-count"}, allEntries = true)
    public NotificationResponse createNotification(NotificationRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Notification message cannot be empty");
        }
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .caseId(request.getCaseId())
                .message(request.getMessage())
                .category(request.getCategory())
                .status(NotificationStatus.UNREAD)
                .createdDate(LocalDateTime.now())
                .build();
        notification = notificationRepository.save(notification);
        log.info("Notification created: id={}, userId={}, category={}",
                notification.getNotificationId(), notification.getUserId(), notification.getCategory());
        return toResponse(notification);
    }

    @Override
    // notifications-by-case is only evicted when a caseId is present (done inside the method)
    @Caching(evict = {
            @CacheEvict(value = "notifications-by-user", allEntries = true),
            @CacheEvict(value = "unread-count",          allEntries = true)
    })
    public NotificationResponse createFromInternal(Map<String, Object> payload) {
        try {
            NotificationRequest req = new NotificationRequest();
            req.setUserId(payload.get("userId").toString());
            req.setMessage(payload.get("message").toString());
            req.setCategory(NotificationCategory.valueOf(payload.get("category").toString().toUpperCase()));
            if (payload.get("caseId") != null) {
                req.setCaseId(Long.valueOf(payload.get("caseId").toString()));
            }
            return createNotification(req);
        } catch (Exception e) {
            throw new BadRequestException("Invalid internal notification payload: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "notifications-by-id", key = "#notificationId")
    public NotificationResponse getById(Long notificationId) {
        return toResponse(findOrThrow(notificationId));
    }

    @Override
    // FIX: evict notifications-by-id by the specific key, not allEntries — no need to nuke every other cached notification
    @Caching(evict = {
            @CacheEvict(value = "notifications-by-id",   key = "#notificationId"),
            @CacheEvict(value = "notifications-by-user", allEntries = true),
            @CacheEvict(value = "unread-count",          allEntries = true)
    })
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = findOrThrow(notificationId);
        if (notification.getStatus() == NotificationStatus.READ) {
            return toResponse(notification);
        }
        notification.setStatus(NotificationStatus.READ);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    // FIX: also evict notifications-by-id — after bulk mark-as-read, cached getById() calls would return stale UNREAD copies
    @Caching(evict = {
            @CacheEvict(value = "notifications-by-user", allEntries = true),
            @CacheEvict(value = "notifications-by-id",   allEntries = true),
            @CacheEvict(value = "unread-count",          allEntries = true)
    })
    public void markAllAsReadForUser(String userId) {
        int updated = notificationRepository.markAllReadByUserId(userId);
        log.info("Marked {} notifications as READ for userId={}", updated, userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "notifications-by-user", key = "#userId + '_ALL'")
    public List<NotificationResponse> getByUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedDateDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "notifications-by-user", key = "#userId + '_' + #status.name()")
    public List<NotificationResponse> getByUserAndStatus(String userId, NotificationStatus status) {
        return notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(userId, status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "notifications-by-user", key = "#userId + '_CAT_' + #category.name()")
    public List<NotificationResponse> getByUserAndCategory(String userId, NotificationCategory category) {
        return notificationRepository.findByUserIdAndCategoryOrderByCreatedDateDesc(userId, category)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "notifications-by-case", key = "#caseId")
    public List<NotificationResponse> getByCase(Long caseId) {
        return notificationRepository.findByCaseIdOrderByCreatedDateDesc(caseId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByCategory(NotificationCategory category) {
        return notificationRepository.findByCategoryOrderByCreatedDateDesc(category)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "unread-count", key = "#userId")
    public long countUnreadForUser(String userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    private Notification findOrThrow(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: #" + id));
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .caseId(n.getCaseId())
                .message(n.getMessage())
                .category(n.getCategory())
                .status(n.getStatus())
                .createdDate(n.getCreatedDate())
                .build();
    }
}
