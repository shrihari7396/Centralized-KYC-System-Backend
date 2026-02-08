package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConsentSnapshot {
    private UUID consentId;
    private UUID userId;
    private String institutionId;
    private String purpose;
    private List<String> allowedAttributes;
    private List<String> allowedDocuments;
    private LocalDateTime consentGivenAt;
    private LocalDateTime consentExpiresAt;
}
