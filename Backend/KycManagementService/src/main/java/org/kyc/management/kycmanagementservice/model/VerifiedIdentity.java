package org.kyc.management.kycmanagementservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VerifiedIdentity {
    private UUID identityId;
    private UUID userId;
    private String fullName;
    private String dateOfBirth;
    private String gender; // optional
    private LocalDateTime verifiedAt;
    private String verifiedBy; // DIGILOCKER
}
