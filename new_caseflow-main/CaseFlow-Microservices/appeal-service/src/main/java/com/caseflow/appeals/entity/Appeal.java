package com.caseflow.appeals.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appeals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appealId;

    @Column(nullable = false)
    private Long caseId;

    @Column(nullable = false, length = 50)
    private String filedByUserId;

    @Column(nullable = false)
    private LocalDateTime filedDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppealStatus status;

    @Version
    private Long version;

    public enum AppealStatus {SUBMITTED, REVIEWED, DECIDED, CANCELLED}
}
