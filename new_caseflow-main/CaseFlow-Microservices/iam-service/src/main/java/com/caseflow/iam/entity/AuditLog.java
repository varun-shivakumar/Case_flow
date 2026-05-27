package com.caseflow.iam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String action;

    private String resource;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}

