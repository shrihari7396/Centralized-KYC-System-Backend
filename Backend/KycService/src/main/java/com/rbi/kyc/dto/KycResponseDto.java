package com.rbi.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycResponseDto {

    private Long kycRequestId;
    private String status; // PENDING, VERIFIED, REJECTED
    private String message;
    private LocalDateTime timestamp;
}
