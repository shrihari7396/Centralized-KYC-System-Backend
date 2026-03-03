package com.rbi.otp.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpGeneratedEvent {

    private Long userId;
    private Long otpRequestId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
