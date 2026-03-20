package org.kyc.management.bankservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationDto {
    private String email;
    private String otp;
    private String role;
    private String firstName;
    private String lastName;
}
