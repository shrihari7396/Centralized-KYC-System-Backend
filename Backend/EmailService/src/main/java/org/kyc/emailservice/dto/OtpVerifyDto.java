package org.kyc.emailservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyDto {

    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "OTP Request ID is required")
    private Long otpRequestId;

    @NotBlank(message = "OTP Code is required")
    private String otpCode;
}
