package com.caseflow.workflow.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "workflow_stages") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowStage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long stageId;
    @Column(nullable = false) private Long caseId;
    @Column(nullable = false) private Integer sequenceNumber;
    @Column(nullable = false) private String roleResponsible;
    @Column(nullable = false) private Integer slaDays;
    @Column(nullable = false) private String stageName;
    @Column(nullable = false) private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    @Column(nullable = false) private Boolean active;

    // skip tracking
    @Column(nullable = false) @Builder.Default
    private Boolean skipped = false;

    @Column(columnDefinition = "TEXT")
    private String skipReason;

    // stores the previous role when reassigned (audit trail)
    private String previousRole;
}
