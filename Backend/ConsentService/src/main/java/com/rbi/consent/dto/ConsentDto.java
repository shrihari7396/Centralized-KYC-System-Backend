package com.rbi.consent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentDto {

    private Long consentId;
    private Long userId;
    private String dataUsage;
    private String purpose;
    private String status; // REQUESTED/GIVEN/WITHDRAWN
    private LocalDateTime grantedAt;
}
