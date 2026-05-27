package com.caseflow.cases.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Case {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long caseId;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String litigantId;
    private String lawyerId;
    @Column(nullable = false) private LocalDateTime filedDate;
    @Column(name = "closed_date") private LocalDateTime closedDate;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private CaseStatus status;
    @Column(name = "case_type") private String caseType;

    public enum CaseStatus { FILED, ACTIVE, ADJOURNED, CLOSED }
}
