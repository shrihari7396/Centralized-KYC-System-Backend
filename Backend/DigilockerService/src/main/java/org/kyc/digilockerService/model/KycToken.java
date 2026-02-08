package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

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
