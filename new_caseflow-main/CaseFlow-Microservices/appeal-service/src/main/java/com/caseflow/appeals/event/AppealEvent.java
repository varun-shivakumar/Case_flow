package com.caseflow.appeals.event;

import java.util.Set;

/**
 * Domain event published when an appeal transitions through its lifecycle.
 * Consumed by {@link AppealEventListener} after the surrounding transaction
 * commits — guarantees notifications never fire for rolled-back changes.
 *
 * recipientUserIds carries the full stakeholder set (filer, opposing party,
 * assigned judge, etc.); the listener fans out one notification per recipient.
 * Nulls and blanks are filtered, duplicates collapse via Set semantics.
 */
public record AppealEvent(
    Type type,
    Long appealId,
    Long caseId,
    Set<String> recipientUserIds,
    String message
) {
    public enum Type {
        FILED,
        CANCELLED,
        UNDER_REVIEW,
        DECIDED
    }
}
