package com.rbi.otp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyResponseDto {

    private Boolean verified;
    private LocalDateTime timestamp;
    private String message;
}
