package org.kyc.management.kycmanagementservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
