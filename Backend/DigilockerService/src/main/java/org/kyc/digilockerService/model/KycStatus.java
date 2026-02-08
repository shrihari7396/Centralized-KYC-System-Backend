package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Builder
public class KycStatus {
    private UUID kycStatusId;
    private UUID userId;
    private String status; // PENDING, VERIFIED, FAILED
    private LocalDateTime verifiedAt;
}
