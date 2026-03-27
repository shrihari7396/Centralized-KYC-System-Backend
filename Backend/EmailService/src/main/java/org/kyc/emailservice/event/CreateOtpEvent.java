package org.kyc.emailservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOtpEvent {
    private String firstName;
    private String lastName;
    private String email;
    private String otp;
    private String serviceType; // e.g., "LOGIN", "DIGILOCKER", "CONSENT"
    private String consentId;
}
