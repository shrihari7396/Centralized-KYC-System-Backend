package org.kyc.management.kycmanagementservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class KycStatus {
    private UUID kycStatusId;
    private UUID userId;
    private String status; // PENDING, VERIFIED, FAILED
    private LocalDateTime verifiedAt;
}
