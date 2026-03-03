package org.kyc.management.kycmanagementservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "kyc_cases")
public class KycCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long caseId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String status; // INITIATED, IN_REVIEW, COMPLETED, REJECTED

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime initiatedAt;

    private LocalDateTime completedAt;
}
