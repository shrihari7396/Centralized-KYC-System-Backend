package org.kyc.digilockerService.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityVerification {

    private String requestId;
    private DocumentType documentType;
    private String documentNumber;
    private String otp;
    private LocalDateTime otpExpiry;
    private String verificationToken;
    private LocalDateTime tokenExpiry;
}
