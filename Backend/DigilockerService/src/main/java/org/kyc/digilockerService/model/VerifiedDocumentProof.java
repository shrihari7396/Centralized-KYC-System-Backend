package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Builder
public class VerifiedDocumentProof {
    private UUID proofId;
    private UUID userId;
    private String documentType; // AADHAAR, PAN, DRIVING_LICENSE
    private String issuer; // UIDAI, ITD, RTO
    private String maskedIdentifier; // e.g. XXXX1234
    private String documentHash; // SHA-256 (simulated)
    private boolean digitallySigned; // true
    private LocalDateTime verifiedAt;
}
