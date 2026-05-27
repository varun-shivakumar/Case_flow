package com.caseflow.appeals.entity;

import com.caseflow.appeals.entity.Appeal.AppealStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Append-only audit trail of every state-changing action taken on an appeal.
 * Required for legal compliance: who did what to which appeal and when.
 *
 * Rows are never updated or deleted — appellate audit history is permanent.
 */
@Entity
@Table(name = "appeal_audit", indexes = {
    @Index(name = "idx_audit_appeal_id", columnList = "appeal_id"),
    @Index(name = "idx_audit_actor",     columnList = "actor_user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "appeal_id", nullable = false)
    private Long appealId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Action action;

    @Column(name = "actor_user_id", nullable = false, length = 50)
    private String actorUserId;

    @Column(name = "actor_role", length = 20)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private AppealStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private AppealStatus toStatus;

    /** Free-form context: outcome, judgeId, document filename, etc. */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public enum Action {
        FILED,
        CANCELLED,
        OPENED_FOR_REVIEW,
        OUTCOME_DRAFT_UPDATED,
        DECIDED,
        DOCUMENT_UPLOADED,
        DOCUMENT_DELETED
    }
}
