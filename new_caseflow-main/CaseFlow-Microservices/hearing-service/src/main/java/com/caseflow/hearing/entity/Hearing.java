package com.caseflow.hearing.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "hearings") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Hearing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long hearingId;
    @Column(nullable = false) private Long caseId;
    @Column(nullable = false) private String judgeId;
    @Column(nullable = false) private LocalDate hearingDate;
    @Column(nullable = false) private String hearingTime;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private HearingStatus status;
    @Column(nullable = false) private String scheduledBy;
    private String rescheduleReason;
    private String hearingNotes;
    public enum HearingStatus { SCHEDULED, RESCHEDULED, COMPLETED }
}
