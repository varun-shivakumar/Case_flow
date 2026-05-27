package com.caseflow.appeals.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "appeal_id", nullable = false)
    private Long appealId;

    @Column(name = "judge_id", nullable = false, length = 50)
    private String judgeId;

    /**
     * User-id of the actor (typically a CLERK or ADMIN) who opened this review and
     * assigned the judge. Nullable for backward-compatibility with reviews created
     * before this column existed.
     */
    @Column(name = "assigned_by_clerk_id", length = 50)
    private String assignedByClerkId;

    @Column(name = "review_date", nullable = false)
    private LocalDateTime reviewDate;

    @Convert(converter = ReviewOutcomeConverter.class)
    @Column(length = 30)
    private ReviewOutcome outcome;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Version
    private Long version;

    public enum ReviewOutcome {APPROVED, REJECTED}
}
