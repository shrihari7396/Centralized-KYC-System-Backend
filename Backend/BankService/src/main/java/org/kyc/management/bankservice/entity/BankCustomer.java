package org.kyc.management.bankservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;
import java.util.UUID;

@Data
@Entity
@Table(name = "bank_customers")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCustomer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private UUID bankId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;

    private Long kycRequestId;
    private String riskLevel;
    private String overrideReason;
    private Timestamp verifiedAt;

    @Column(columnDefinition = "TEXT")
    private String consentToken; // JWT token from ConsentService

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
}
