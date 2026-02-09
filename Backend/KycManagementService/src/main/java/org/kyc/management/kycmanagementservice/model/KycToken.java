package org.kyc.management.kycmanagementservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class KycToken {
    private UUID kycTokenId;
    private UUID userId;
    private List<String> allowedAttributes; // NAME, DOB, ADDRESS
    private List<String> allowedDocuments; // AADHAAR, PAN, DL
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private String status; // ACTIVE, EXPIRED, REVOKED
    private String tokenHash;
    private String signature;
}
