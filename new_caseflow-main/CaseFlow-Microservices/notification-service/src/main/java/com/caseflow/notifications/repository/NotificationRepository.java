package com.caseflow.notifications.repository;

import com.caseflow.notifications.entity.Notification;
import com.caseflow.notifications.enums.NotificationCategory;
import com.caseflow.notifications.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedDateDesc(String userId);

    List<Notification> findByUserIdAndStatusOrderByCreatedDateDesc(String userId, NotificationStatus status);

    List<Notification> findByCaseIdOrderByCreatedDateDesc(Long caseId);

    List<Notification> findByCategoryOrderByCreatedDateDesc(NotificationCategory category);

    List<Notification> findByUserIdAndCategoryOrderByCreatedDateDesc(String userId, NotificationCategory category);

    long countByUserIdAndStatus(String userId, NotificationStatus status);

    // clearAutomatically = true flushes the JPA first-level cache after the bulk UPDATE
    // so subsequent reads in the same transaction see the correct READ status
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.userId = :userId AND n.status = 'UNREAD'")
    int markAllReadByUserId(@Param("userId") String userId);
}
