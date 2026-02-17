package org.kyc.digilockerService.dto;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
public class OtpVerifyRequest {
    @NotBlank(message = "Request ID is required")
    private String requestId;

    @NotBlank(message = "Document number is required")
    private String documentNumber;

    @NotBlank(message = "OTP is required")
    private String otp;
}
