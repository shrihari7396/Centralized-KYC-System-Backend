package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

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
