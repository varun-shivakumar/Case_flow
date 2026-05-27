package com.caseflow.notifications.service;

import com.caseflow.notifications.entity.Notification;
import com.caseflow.notifications.enums.*;
import com.caseflow.notifications.exception.*;
import com.caseflow.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private NotificationServiceImpl notificationService;

    @Test void getById_notFound_throws() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> notificationService.getById(99L));
    }

    @Test void getByUser_empty() {
        when(notificationRepository.findByUserId(1L)).thenReturn(List.of());
        assertEquals(0, notificationService.getByUser(1L).size());
    }

    @Test void countUnread() {
        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.UNREAD)).thenReturn(5L);
        assertEquals(5L, notificationService.countUnreadForUser(1L));
    }
}
