package org.kyc.digilockerService.dto;

import lombok.Builder;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpVerifyResponse {
    private String status;
    private String verificationToken;
    private String reason;
}
