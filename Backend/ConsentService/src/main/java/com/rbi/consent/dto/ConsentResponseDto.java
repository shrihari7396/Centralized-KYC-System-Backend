package com.rbi.consent.dto;

import com.rbi.consent.enums.ConsentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponseDto {

    private UUID consentId;
    private ConsentStatus status;
    private String jwtToken;
    private LocalDateTime expiryAt;
}
