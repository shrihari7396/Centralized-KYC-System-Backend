package com.rbi.userservice.DTO;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OtpVerificationDto {
    private String otp;
    private String email;
}
