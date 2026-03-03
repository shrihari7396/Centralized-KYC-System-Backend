package com.rbi.consent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "consent_records")
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String dataUsage;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String status; // REQUESTED, GIVEN, WITHDRAWN

    private LocalDateTime grantedAt;

    @Column(nullable = false)
    private Date expiresAt;
}
