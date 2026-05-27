package com.caseflow.compliance.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "audits") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Audit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long auditId;
    @Column(nullable = false) private String adminId;
    @Column(nullable = false) private String scope;
    @Column(columnDefinition = "TEXT") private String findings;
    @Column(nullable = false) private LocalDate date;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private AuditStatus status;
    public enum AuditStatus { OPEN, CLOSED }
}
