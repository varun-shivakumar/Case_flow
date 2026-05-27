package com.caseflow.compliance.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "compliance_records") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplianceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long complianceId;
    @Column(nullable = false) private Long caseId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ComplianceType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private ComplianceResult result;
    @Column(nullable = false) private LocalDate date;
    @Column(columnDefinition = "TEXT") private String notes;
    /**
     * UUID identifying the compliance-check run that created this record.
     * Every record produced by a single invocation of "Run Compliance Check"
     * shares the same runId so the UI can group them as one entry.
     */
    @Column(name = "run_id", length = 64) private String runId;
    /**
     * Precise instant the run was started. Same for every record of one run.
     * Lets the UI show date AND time and distinguish multiple runs on the same day.
     */
    @Column(name = "run_date") private LocalDateTime runDate;
    public enum ComplianceType { DOCUMENT, PROCESS }
    public enum ComplianceResult { PASS, FAIL }
}
