package org.kyc.digilockerService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpVerifyRequest {
    private String mobileNumber;
    private String otp;
    private boolean consentAccepted;
}
