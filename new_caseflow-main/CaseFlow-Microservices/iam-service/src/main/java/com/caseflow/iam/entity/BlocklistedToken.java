package com.caseflow.iam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocklisted_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlocklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private LocalDateTime blockedAt;

    @Column(nullable = false)
    private LocalDateTime expiryDate;
}

