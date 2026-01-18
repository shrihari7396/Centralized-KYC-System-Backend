package com.rbi.userservice.DTO;

import com.rbi.userservice.Model.Role;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OtpVerificationDto {
    private String otp;
    private String email;
}
