package com.caseflow.appeals.event;

import com.caseflow.appeals.client.NotificationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Set;

/**
 * Fires notifications only AFTER the underlying transaction commits.
 * Prior implementation called the notification client inline inside the
 * transaction — a transaction rollback would still notify the user of an
 * action that never persisted.
 *
 * Each event carries a stakeholder set; this listener fans out one
 * notification per recipient, surviving individual delivery failures.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppealEventListener {

    private final NotificationServiceClient notificationClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AppealEvent event) {
        Set<String> recipients = event.recipientUserIds();
        if (recipients == null || recipients.isEmpty()) {
            log.warn("{} event for appeal #{} has no recipients — skipping notification dispatch",
                event.type(), event.appealId());
            return;
        }
        for (String userId : recipients) {
            if (userId == null || userId.isBlank()) continue;
            try {
                notificationClient.sendNotification(Map.of(
                    "userId",   userId,
                    "caseId",   event.caseId(),
                    "category", "APPEAL",
                    "message",  event.message()
                ));
            } catch (Exception e) {
                log.warn("Failed to dispatch {} notification for appeal #{} to [{}]: {}",
                    event.type(), event.appealId(), userId, e.getMessage());
            }
        }
    }
}
